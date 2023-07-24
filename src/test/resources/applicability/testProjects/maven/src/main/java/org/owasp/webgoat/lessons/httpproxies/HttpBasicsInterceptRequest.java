/*
 * This file is part of WebGoat, an Open Web Application Security Project utility. For details, please see http://www.owasp.org/
 *
 * Copyright (c) 2002 - 2019 Bruce Mayhew
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * Getting Source ==============
 *
 * Source for this application is maintained at https://github.com/WebGoat/WebGoat, a repository for free software projects.
 */

package org.owasp.webgoat.lessons.httpproxies;

import javax.servlet.http.HttpServletRequest;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HttpBasicsInterceptRequest extends AssignmentEndpoint {

  @RequestMapping(
      path = "/HttpProxies/intercept-request",
      method = {RequestMethod.POST, RequestMethod.GET})
  @ResponseBody
  public AttackResult completed(
      @RequestHeader(value = "x-request-intercepted", required = false) Boolean headerValue,
      @RequestParam(value = "changeMe", required = false) String paramValue,
      HttpServletRequest request) {
    if (HttpMethod.POST.matches(request.getMethod())) {
      return failed(this).feedback("http-proxies.intercept.failure").build();
    }
    if (headerValue != null
        && paramValue != null
        && headerValue
        && "Requests are tampered easily".equalsIgnoreCase(paramValue)) {
      return success(this).feedback("http-proxies.intercept.success").build();
    } else {
      return failed(this).feedback("http-proxies.intercept.failure").build();
    }
  }
}
