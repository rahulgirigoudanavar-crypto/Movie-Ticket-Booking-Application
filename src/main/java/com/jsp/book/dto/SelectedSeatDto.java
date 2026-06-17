package com.jsp.book.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectedSeatDto {

    private String seatNumber;
    private String category;
    private Double price;
}
