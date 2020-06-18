/*
 * Copyright @ Atlassian Pty Ltd
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

import org.jitsi.meet.test.base.*;
import org.jitsi.meet.test.web.*;
import org.testng.annotations.*;

/**
 * Joins conference with 2 participants and checks if the media connection is
 * successfully established. Then hangups the call.
 */
public class ClientPerformanceTest
    extends WebTestBase
{
    /**
     * Joins the conference, sits there for 60 sec and ends the call.
     */
    @Test
    public void testConference()
    {
        joinFirstParticipant();
        joinSecondParticipant();
        joinThirdParticipant();

        try
        {
            Thread.sleep(60000);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        participants.getAll().forEach(Participant::hangUp);
    }
}
