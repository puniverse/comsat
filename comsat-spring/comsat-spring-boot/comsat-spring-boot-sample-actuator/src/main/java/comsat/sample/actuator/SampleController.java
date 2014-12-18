/*
 * COMSAT
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
/*
 * Based on the corresponding class in Spring Boot Samples.
 * Copyright the original author(s).
 * Released under the ASF 2.0 license.
 */
package comsat.sample.actuator;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Description("A controller for handling requests for hello messages")
public class SampleController {

    @Autowired
    private HelloWorldService helloWorldService;

    @RequestMapping(value = "/helloThreadBlocking", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, String> helloThreadBlocking() {
        return Collections.singletonMap("message",
                this.helloWorldService.getHelloMessage());
    }

    // snippet suspendable
    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, String> hello() throws SuspendExecution, InterruptedException {
        Fiber.sleep(100);
        return Collections.singletonMap("message",
                this.helloWorldService.getHelloMessage());
    }
    // end of snippet

    @RequestMapping(value = "/", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> olleh(@Validated Message message) throws SuspendExecution, InterruptedException {
        Fiber.sleep(100);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("message", message.getValue());
        model.put("title", "Hello Home");
        model.put("date", new Date());
        return model;
    }

    @RequestMapping("/foo")
    @ResponseBody
    public String foo() throws SuspendExecution, InterruptedException {
        Fiber.sleep(100);
        throw new IllegalArgumentException("Server error");
    }

    protected static class Message {

        @NotBlank(message = "Message value cannot be empty")
        private String value;

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
