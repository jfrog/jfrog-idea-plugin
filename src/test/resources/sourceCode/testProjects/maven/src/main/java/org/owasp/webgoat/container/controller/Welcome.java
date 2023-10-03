/**
 * ************************************************************************************************
 *
 * <p>This file is part of WebGoat, an Open Web Application Security Project utility. For details,
 * please see http://www.owasp.org/
 *
 * <p>Copyright (c) 2002 - 2014 Bruce Mayhew
 *
 * <p>This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * <p>Getting Source ==============
 *
 * <p>Source for this application is maintained at https://github.com/WebGoat/WebGoat, a repository
 * for free software projects.
 *
 * @author WebGoat
 * @since October 28, 2003
 * @version $Id: $Id
 */
package org.owasp.webgoat.container.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Welcome class.
 *
 * @author rlawson
 * @version $Id: $Id
 */
@Controller
public class Welcome {

  private static final String WELCOMED = "welcomed";

  /**
   * welcome.
   *
   * @param request a {@link javax.servlet.http.HttpServletRequest} object.
   * @return a {@link org.springframework.web.servlet.ModelAndView} object.
   */
  @GetMapping(path = {"welcome.mvc"})
  public ModelAndView welcome(HttpServletRequest request) {

    // set the welcome attribute
    // this is so the attack servlet does not also
    // send them to the welcome page
    HttpSession session = request.getSession();
    if (session.getAttribute(WELCOMED) == null) {
      session.setAttribute(WELCOMED, "true");
    }

    // go ahead and send them to webgoat (skip the welcome page)
    ModelAndView model = new ModelAndView();
    model.setViewName("forward:/attack?start=true");
    return model;
  }
}
