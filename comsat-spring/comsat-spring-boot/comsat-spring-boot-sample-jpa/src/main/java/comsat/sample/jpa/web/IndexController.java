/*
 * COMSAT
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
 * Copyright the original author Dave Syer.
 * Released under the ASF 2.0 license.
 */
package comsat.sample.jpa.web;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import comsat.sample.jpa.domain.Note;
import comsat.sample.jpa.repository.NoteRepository;

@Controller
public class IndexController {
    @Autowired
    private NoteRepository noteRepository;

    @RequestMapping("/")
    @Transactional(readOnly = true)
    public ModelAndView index() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
        List<Note> notes = this.noteRepository.findAll();
        ModelAndView modelAndView = new ModelAndView("index");
        modelAndView.addObject("notes", notes);
        return modelAndView;
    }
}
