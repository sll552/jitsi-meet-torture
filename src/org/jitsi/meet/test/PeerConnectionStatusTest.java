/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.meet.test;

import junit.framework.*;

import org.jitsi.meet.test.util.*;

import org.openqa.selenium.*;

import java.util.*;

/**
 * The test checks the UI for displaying notifications about participants media
 * connectivity status(connection between the peer and the JVB).
 *
 * 1. Join with 2 participants {@link #testInitialize()}.
 * 2. Block media flow on 2nd and check if is indicated as disconnected
 *    {@link #test2ndPeerInterruptedDuringCall()}.
 * 3. Unblock the ports and see if the UI updated accordingly
 *    {@link #test2ndPeerRestored()}.
 * 4. Block the 2nd peer media ports and wait until channels expire on
 *    the bridge then join with the 3rd participant and test switching between
 *    participants {@link #testJoin3rdWhile2ndExpired()}.
 * 5. Re-join 2nd with "failICE" and see if others are notified
 *    {@link #test2ndPeerInterruptedDuringCall()}. This checks if the JVB will
 *    send disconnected notification for the participant who has never
 *    connected, but for whom the channels have been allocated.
 *
 * @author Pawel Domas
 */
public class PeerConnectionStatusTest
    extends TestCase
{
    /**
     * Name of the system property which point to the firewall script used to
     * block the ports. See {@link #firewallScript}.
     */
    private static final String FIREWALL_SCRIPT_PROP_NAME = "firewall.script";

    /**
     * Stores the path to the firewall script. It is expected that the script
     * supports two commands:
     *
     * 1. "--block-port {port number}" will adjust firewall rules to block both
     *    UDP and TCP (inbound+outbound) traffic on the given port number.
     *    If the script is called twice with different ports it is not important
     *    if the previously blocked port gets unblocked.
     *    MUST always drop traffic from any to JVB's TCP port(4443 by default)
     *
     * 2. "--clear-rules" will remove the rules for the all previously blocked
     *    ports.
     */
    private static String firewallScript;

    /**
     * Stores the RTP bundle port number of the 2nd participant.
     */
    private static String peer2bundlePort;

    /**
     * Creates new instance of <tt>PeerConnectionStatusTest</tt> for the given
     * test method name.
     *
     * @param testMethodName the name of the test method which will be assigned
     * to the new instance.
     */
    public PeerConnectionStatusTest(String testMethodName)
    {
        super(testMethodName);
    }

    /**
     * Constructs the peer connection status test suite.
     */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite();

        firewallScript = System.getProperty(FIREWALL_SCRIPT_PROP_NAME);

        if(firewallScript == null)
        {
            return suite;
        }

        suite.addTest(
            new PeerConnectionStatusTest("testInitialize"));
        suite.addTest(
            new PeerConnectionStatusTest("test2ndPeerInterruptedDuringCall"));
        suite.addTest(
            new PeerConnectionStatusTest("test2ndPeerRestored"));
        suite.addTest(
            new PeerConnectionStatusTest("test2ndPeerExpired"));
        suite.addTest(
            new PeerConnectionStatusTest("testJoin3rdWhile2ndExpired"));
        suite.addTest(
            new PeerConnectionStatusTest("test2ndFailsICEOnJoin"));

        return suite;
    }

    /**
     * Calls {@link #firewallScript} to block given port.
     *
     * @param portNumberStr a string which represents the port number to be
     * blocked.
     *
     * @throws Exception if anything goes wrong.
     */
    private static void blockPort(String portNumberStr)
        throws Exception
    {
        // Tr to parse just to see if it's a valid integer
        int portNumber = Integer.parseInt(portNumberStr);
        if (portNumber < 0 || portNumber > 65535)
        {
            throw new Exception("Invalid port number: " + portNumberStr);
        }

        CmdExecutor cmdExecutor = new CmdExecutor();

        List<String> cmdArgs = new LinkedList<>();

        cmdArgs.add(firewallScript);
        cmdArgs.add("--block-port");
        cmdArgs.add(portNumberStr);

        System.err.println("Will block port: " + portNumberStr);

        cmdExecutor.executeCmd(cmdArgs);
    }

    /**
     * Unblocks all previously blocked ports.
     *
     * @throws Exception if anything goes wrong.
     */
    private static void clearFirewallRules()
        throws Exception
    {
        CmdExecutor cmdExecutor = new CmdExecutor();

        List<String> cmdArgs = new LinkedList<>();

        cmdArgs.add(firewallScript);
        cmdArgs.add("--clear-rules");

        System.err.println("Will unblock all previously blocked ports");

        cmdExecutor.executeCmd(cmdArgs);
    }

    /**
     * Makes sure that there are 2 participants in the conference.
     */
    public void testInitialize()
    {
        ConferenceFixture.ensureTwoParticipants();
    }

    /**
     * Checks the UI indication for the participant whose connection is
     * disrupted during the call.
     *
     * @throws Exception if something goes wrong.
     */
    public void test2ndPeerInterruptedDuringCall()
        throws Exception
    {
        WebDriver owner = ConferenceFixture.getOwnerInstance();
        assertNotNull(owner);
        WebDriver secondPeer = ConferenceFixture.getSecondParticipantInstance();
        assertNotNull(secondPeer);

        // 1. Block media flow on 2nd and check if is indicated as disconnected
        peer2bundlePort = MeetUtils.getBundlePort(secondPeer);
        System.err.println(
            "Local bundle port for 2: " + peer2bundlePort);
        blockPort(peer2bundlePort);

        // 2. Select 2nd participant on Owner
        MeetUIUtils.selectRemoteVideo(owner, secondPeer);
        // At this point user 2 thumb should be a display name
        MeetUIUtils.assertDisplayNameVisible(owner, secondPeer);

        // Check if 2nd user is marked as "disconnected" from 1st peer view
        MeetUIUtils.verifyUserConnStatusIndication(
                owner, secondPeer, false /* disconnected */);
        MeetUIUtils.assertLargeVideoIsGrey(owner);

        // 3. Switch to local video
        MeetUIUtils.selectLocalVideo(owner);
        // Now we're expecting to see gray video thumbnail
        MeetUIUtils.assertGreyVideoThumbnailDisplayed(owner, secondPeer);

        // Check also 2nd participant's local UI for indication
        MeetUIUtils.verifyLocalConnStatusIndication(
                secondPeer, false /* disconnected */);

        // 4. Select remote again
        MeetUIUtils.selectRemoteVideo(owner, secondPeer);

        // Mute video(this is not real live scenario, but we want to make
        // sure that the grey avatar is displayed if the connection gets
        // disrupted while video muted)
        new StopVideoTest("stopVideoOnParticipantAndCheck")
            .stopVideoOnParticipantAndCheck();
        MeetUIUtils.assertGreyAvatarOnLarge(owner);

        // The avatar should remain
        new StopVideoTest("startVideoOnParticipantAndCheck")
            .startVideoOnParticipantAndCheck();
        MeetUIUtils.assertGreyAvatarOnLarge(owner);
    }

    /**
     * Unblocks 2nd participant's ports and see if UI has been updated
     * accordingly.
     *
     * @throws Exception if anything unexpected happens.
     */
    public void test2ndPeerRestored()
        throws Exception
    {
        WebDriver owner = ConferenceFixture.getOwnerInstance();
        WebDriver secondPeer = ConferenceFixture.getSecondParticipantInstance();

        // 1. Unlock the port and see if we recover
        clearFirewallRules();

        // 2. Verify if connection has restored
        MeetUIUtils.verifyUserConnStatusIndication(owner, secondPeer, true);
        MeetUIUtils.assertLargeVideoNotGrey(owner);

        // 3. Check local status
        MeetUIUtils.verifyLocalConnStatusIndication(secondPeer, true);
    }

    /**
     * Blocks 2nd participant's ports until they expire on the JVB and then join
     * with 3rd to see if the notification has been sent as expected.
     *
     * @throws Exception if something goes wrong
     */
    public void test2ndPeerExpired()
        throws Exception
    {
        WebDriver owner = ConferenceFixture.getOwnerInstance();
        assertNotNull(owner);
        WebDriver secondPeer = ConferenceFixture.getSecondParticipantInstance();
        assertNotNull(secondPeer);

        // The purpose of next steps is to check if JVB sends
        // "interrupted" notifications about the user's whose channels have
        // expired.

        // Block the port and wait for the channels to expire on
        // the bridge(unrecoverable)
        blockPort(peer2bundlePort);
        MeetUIUtils.verifyUserConnStatusIndication(owner, secondPeer, false);
        MeetUIUtils.verifyLocalConnStatusIndication(secondPeer, false);

        // FIXME it would be better to expire channels somehow
        TestUtils.waitMillis(65000);
    }

    /**
     * Join with the 3rd participant while 2nd stays in the room with expired
     * channels. The point of these is to make sure that the JVB has notified
     * the 3rd peer correctly.
     */
    public void testJoin3rdWhile2ndExpired()
        throws Exception
    {
        // 8. Join with 3rd and see if the user is marked as interrupted
        ConferenceFixture.waitForThirdParticipantToConnect();

        WebDriver owner = ConferenceFixture.getOwnerInstance();
        assertNotNull(owner);
        WebDriver secondPeer = ConferenceFixture.getSecondParticipantInstance();
        assertNotNull(secondPeer);
        WebDriver thirdPeer = ConferenceFixture.getThirdParticipantInstance();
        assertNotNull(thirdPeer);

        MeetUIUtils.verifyUserConnStatusIndication(
            thirdPeer, secondPeer, false);
        MeetUIUtils.verifyUserConnStatusIndication(
            thirdPeer, owner, true);

        // Select local video
        MeetUIUtils.selectLocalVideo(thirdPeer);
        // User 2 should be a grey avatar
        MeetUIUtils.assertGreyAvatarDisplayed(thirdPeer, secondPeer);
        // Select disconnected participant
        MeetUIUtils.selectRemoteVideo(thirdPeer, secondPeer);
        MeetUIUtils.assertGreyAvatarOnLarge(thirdPeer);

        // Unblock the port
        clearFirewallRules();
    }

    /**
     * Closes 2nd participant and joins with special flag "failICE" set to true
     * in order to fail ICE initially. The bridge should notify others when
     * the user fails to establish ICE connection.
     */
    public void test2ndFailsICEOnJoin()
    {
        WebDriver owner = ConferenceFixture.getOwnerInstance();
        assertNotNull(owner);
        WebDriver secondPeer = ConferenceFixture.getSecondParticipantInstance();
        assertNotNull(secondPeer);
        WebDriver thirdPeer = ConferenceFixture.getThirdParticipantInstance();
        assertNotNull(thirdPeer);

        // Close 2nd and join with failICE=true
        ConferenceFixture.close(secondPeer);
        secondPeer = null;
        ConferenceFixture.startSecondParticipant("config.failICE=true");
        secondPeer = ConferenceFixture.getSecondParticipantInstance();
        assertNotNull(secondPeer);
        MeetUtils.waitForParticipantToJoinMUC(secondPeer, 10);
        // It will be marked as disconnected only after 15 seconds since
        // the channels have been allocated.
        TestUtils.waitMillis(17000);
        // Now see if others will see him
        MeetUIUtils.verifyUserConnStatusIndication(
                owner, secondPeer, false);
        MeetUIUtils.verifyUserConnStatusIndication(
                thirdPeer, secondPeer, false);
    }
}
