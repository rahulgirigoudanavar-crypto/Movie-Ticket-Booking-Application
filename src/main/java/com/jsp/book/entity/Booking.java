package com.jsp.book.entity;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String movieName;
    private LocalDate date;
    private String seats;
    private double totalAmount;

    @ManyToOne
    private User user;
}