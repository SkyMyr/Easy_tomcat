package com.tomcat.servlet;

import com.tomcat.annotations.Servlet;
import com.tomcat.baseservlet.AbstractServlet;
import com.tomcat.core.Request;
import com.tomcat.core.Response;

/**
 * @Author: myr
 * @Date: 2019/9/18 17:02
 */
@Servlet("/login/id")
public class LoginServlet extends AbstractServlet {
    @Override
    protected void doGet(Request request, Response response) {
        doPost(request,response);
    }

    @Override
    protected void doPost(Request request, Response response) {
        response.setResponseContent("This is login_servlet!");
    }

    @Override
    public void init() throws Exception {

    }

    @Override
    public void destroy() {

    }
}
