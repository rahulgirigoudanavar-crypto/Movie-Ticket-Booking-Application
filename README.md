# 🎬 Movie Ticket Booking Application

## 📌 Project Overview

The **Movie Ticket Booking Application** is a full-stack web application that allows users to browse movies, view show timings, select seats, and book movie tickets online. The application provides a seamless booking experience with secure user authentication and efficient ticket management.

## 🚀 Features

### User Features

* User Registration and Login
* Browse Available Movies
* View Movie Details and Show Timings
* Select and Book Seats
* View Booking History
* Cancel Bookings
* Responsive User Interface

### Admin Features

* Add, Update, and Delete Movies
* Manage Show Timings
* View All Bookings
* Manage Users

## 🛠️ Technologies Used

### Frontend

* HTML5
* CSS3
* JavaScript
* Bootstrap

### Backend

* Java
* Spring Boot
* Spring MVC
* Spring Data JPA
* Hibernate

### Database

* MySQL

### Tools

* Maven
* Git & GitHub
* Postman
* VS Code / IntelliJ IDEA

## 📂 Project Structure

```text
Movie-Ticket-Booking-Application
│
├── src/main/java
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── config
│
├── src/main/resources
│   ├── application.properties
│   └── templates
│
├── pom.xml
└── README.md
```

## ⚙️ Installation & Setup

### Prerequisites

* Java 17+
* MySQL Server
* Maven
* IDE (IntelliJ IDEA / VS Code)

### Steps

1. Clone the repository

```bash
git clone https://github.com/rahulgirigoudanavar-crypto/Movie-Ticket-Booking-Application.git
```

2. Navigate to the project directory

```bash
cd Movie-Ticket-Booking-Application
```

3. Configure MySQL database in `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/movie_booking
spring.datasource.username=root
spring.datasource.password=your_password
```

4. Build the project

```bash
mvn clean install
```

5. Run the application

```bash
mvn spring-boot:run
```

6. Open the application in your browser

```text
http://localhost:8080
```

## 📊 Database Modules

* User Management
* Movie Management
* Theatre Management
* Show Management
* Seat Management
* Ticket Booking Management

## 🔮 Future Enhancements

* Online Payment Integration
* Email/SMS Notifications
* Movie Reviews and Ratings
* QR Code Based Ticket Verification
* Multi-Theatre Support


