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
package comsat.sample.ui.mvc;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import comsat.sample.ui.Message;
import comsat.sample.ui.MessageRepository;

@Controller
@RequestMapping("/")
public class MessageController {

    private final MessageRepository messageRepository;

    @Autowired
    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @RequestMapping
    public ModelAndView list() throws InterruptedException, SuspendExecution {
        Fiber.sleep(100);
        Iterable<Message> messages = this.messageRepository.findAll();
        return new ModelAndView("messages/list", "messages", messages);
    }

    @RequestMapping("{id}")
    public ModelAndView view(@PathVariable("id") Message message) throws InterruptedException, SuspendExecution {
        Fiber.sleep(100);
        return new ModelAndView("messages/view", "message", message);
    }

    @RequestMapping(params = "form", method = RequestMethod.GET)
    public String createForm(@ModelAttribute Message message) throws InterruptedException, SuspendExecution {
        Fiber.sleep(100);
        return "messages/form";
    }

    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView create(@Valid Message message, BindingResult result,
            RedirectAttributes redirect) throws InterruptedException, SuspendExecution {
        Fiber.sleep(100);
        if (result.hasErrors()) {
            ModelAndView mav = new ModelAndView("messages/form");
            mav.addObject("formErrors", result.getAllErrors());
            mav.addObject("fieldErrors", getFieldErrors(result));
            return mav;
        }
        message = this.messageRepository.save(message);
        redirect.addFlashAttribute("globalMessage", "Successfully created a new message");
        return new ModelAndView("redirect:/{message.id}", "message.id", message.getId());
    }

    private Map<String, ObjectError> getFieldErrors(BindingResult result) throws InterruptedException, SuspendExecution {
        Fiber.sleep(100);
        Map<String, ObjectError> map = new HashMap<String, ObjectError>();
        for (FieldError error : result.getFieldErrors()) {
            map.put(error.getField(), error);
        }
        return map;
    }

    @RequestMapping("foo")
    public String foo() throws InterruptedException, SuspendExecution {
        Fiber.sleep(100);
        throw new RuntimeException("Expected exception in controller");
    }
}
