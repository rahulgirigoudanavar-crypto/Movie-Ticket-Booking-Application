package com.jsp.book.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsp.book.entity.Booking;
import com.jsp.book.entity.User;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ✅ Correct way (NO static, NO body)
    List<Booking> findByUser(User user);
}