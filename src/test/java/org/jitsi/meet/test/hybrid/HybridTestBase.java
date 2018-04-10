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
package org.jitsi.meet.test.hybrid;

import org.jitsi.meet.test.base.*;
import org.jitsi.meet.test.mobile.*;
import org.jitsi.meet.test.web.*;

import java.util.*;

/**
 * A class of the test which utilizes {@link HybridParticipantFactory} which
 * means that it's capable of creating and operating on both
 * {@link MobileParticipant} and {@link WebParticipant}.
 *
 * FIXME work on the templates, so that it's obvious on what types of
 * participants each class of the tests is operating on and that the types are
 * enforced.
 */
public class HybridTestBase
    extends AbstractBaseTest<Participant>
{
    /**
     * Creates {@link HybridParticipantHelper}.
     *
     * {@inheritDoc}
     */
    @Override
    protected ParticipantHelper<Participant> createParticipantHelper(
            Properties config)
    {
        return new HybridParticipantHelper(config);
    }
}
