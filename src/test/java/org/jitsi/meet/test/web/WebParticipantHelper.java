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
package org.jitsi.meet.test.web;

import org.jitsi.meet.test.base.*;

import java.util.*;

/**
 * Web {@link ParticipantHelper}.
 *
 * FIXME we could probably pass a class to the generic helper as the only
 * purpose of this class is to inject the type. To be figured out with the
 * templates.
 */
public class WebParticipantHelper extends ParticipantHelper<WebParticipant>
{
    protected WebParticipantHelper(Properties config)
    {
        super(config);
    }

    @Override
    protected ParticipantFactory<WebParticipant> createFactory()
    {
        return new WebParticipantFactory();
    }
}
