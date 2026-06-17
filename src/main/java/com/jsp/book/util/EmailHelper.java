package com.jsp.book.util;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailHelper {

	private static final String FROM_EMAIL = "bookmy-ticket.com";
	private static final String FROM_NAME = "Book-My-Ticket";
	private static final String SUBJECT = "Otp for Creating Account with BookMyTicket";
	private static final String TEMPLATE = "email-template.html";

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;

	@Async
	public void sendOtp(int otp, String name, String email) {

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);

			helper.setFrom(FROM_EMAIL, FROM_NAME);
			helper.setTo(email);
			helper.setSubject(SUBJECT);

			Context context = new Context();
			context.setVariable("name", name);
			context.setVariable("otp", otp);

			String body = templateEngine.process(TEMPLATE, context);
			helper.setText(body, true);

			mailSender.send(message);
			System.out.println("OTP email sent successfully to: " + email);

		} catch (Exception ex) {
			System.err.println("Failed to send OTP mail for email: " + email + " - Error: " + ex.getMessage());
			// Log the OTP for debugging purposes (remove in production)
			System.out.println("OTP for " + email + ": " + otp + " (email failed, check logs)");
		}
	}

	@Async
	public void sendTicketDetails(com.jsp.book.entity.BookedTicket ticket, com.jsp.book.entity.User user) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);

			helper.setFrom(FROM_EMAIL, FROM_NAME);
			helper.setTo(user.getEmail());
			helper.setSubject("Ticket Booking Confirmation - " + ticket.getMovieName());

			String body = "<h2>Ticket Confirmed!</h2>" +
					"<p>Dear " + user.getName() + ",</p>" +
					"<p>Your booking for <b>" + ticket.getMovieName() + "</b> has been confirmed.</p>" +
					"<p><b>Theater:</b> " + ticket.getTheaterName() + " (" + ticket.getScreenName() + ")</p>" +
					"<p><b>Date & Time:</b> " + ticket.getShowDate() + " at " + ticket.getShowTiming() + "</p>" +
					"<p><b>Seats:</b> " + String.join(", ", ticket.getSeatNumber()) + "</p>" +
					"<p><b>Amount Paid:</b> Rs. " + ticket.getTicketPrice() + "</p>" +
					"<p>Thank you for choosing BookMyTicket!</p>";

			helper.setText(body, true);
			mailSender.send(message);

			System.out.println("\n================ SMS NOTIFICATION ================");
			System.out.println("To: " + user.getMobile());
			System.out.println("Message: Confirmed! " + ticket.getMovieName() + " at " + ticket.getTheaterName() + 
					" on " + ticket.getShowDate() + " " + ticket.getShowTiming() + ". Seats: " + String.join(", ", ticket.getSeatNumber()));
			System.out.println("=================================================\n");

		} catch (Exception ex) {
			System.err.println("Failed to send ticket details mail to: " + user.getEmail() + " - " + ex.getMessage());
		}
	}
}
