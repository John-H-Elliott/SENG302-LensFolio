package nz.ac.canterbury.seng302.portfolio.controller;


import nz.ac.canterbury.seng302.portfolio.service.RegisterClientService;
import nz.ac.canterbury.seng302.portfolio.service.UserAccountService;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private RegisterClientService registerClientService;

    /***
     * Request Mapping Method
     *
     * Handles Errors that are caused by invalid URL's and links them to custom
     * error pages in which can redirect to a proper page
     *
     * @param request HTTP request sent to this endpoint
     * @param principal Authentication principal
     * @param model Parameters sent to thymeleaf template to be rendered into HTML
     * @return HTML page to show an error
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request,
                              @AuthenticationPrincipal AuthState principal,
                              Model model) {


        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            if (statusCode == HttpStatus.FORBIDDEN.value()) {
                return "403Forbidden";
            }

            UserResponse getUserByIdReplyHeader;

            Integer id = userAccountService.getUserIDFromAuthState(principal);
            getUserByIdReplyHeader = registerClientService.getUserData(id);
            String fullNameHeader = getUserByIdReplyHeader.getFirstName() + " " + getUserByIdReplyHeader.getMiddleName() + " " + getUserByIdReplyHeader.getLastName();
            model.addAttribute("headerFullName", fullNameHeader);
            model.addAttribute("userId", id);

            if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return "500InternalServer";
            }
            else if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "404NotFound";
            } else {
                //technically any other error
                return "404NotFound";
            }
        }
        return "error";
    }
}