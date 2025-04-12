# split-payments-system
A backend system built with **Spring Boot** that handles secure multi-parent school payment processing.
It includes custom business logic for dynamic fee calculation, balance validation, and parent-student relationship enforcement.

---



## 📦 Tech Stack

- **Java 11**
- **Spring Boot**
- **PostgreSQL**
- **Lombok**
- **JPA/Hibernate**
- **Maven**

---

## 🚀 Features

- Secure payment processing per parent-student relationship
- Multi-parent contribution logic with dynamic rate adjustments
- Handles partial and failed payments with transaction persistence
- Modular service architecture using Spring components
- Transactional integrity using Spring’s `@Transactional`
---


# Payment Processing System - Design Decisions for Security and Multi-Table Payment Processing

## Security Design Decisions

### 1. **Authentication and Authorization**

- **JWT Authentication:**  
  The system employs JSON Web Token (JWT) for securing endpoints and ensuring that only authorized users can access the system. JWT is issued upon login, and subsequent requests to the backend must include the token in the `Authorization` header. This token is validated for each request to ensure that the user is authenticated.
  
  - **User Login Flow:**  
    1. The user provides their credentials (username and password).
    2. On successful authentication, a JWT token is generated.
    3. The token is returned to the client for use in future requests.
    4. Each subsequent request includes the token in the `Authorization` header.
  
  - **Token Validation:**  
    Every protected endpoint in the system uses a filter to check the JWT's validity. The system ensures the token is not expired and that it is associated with an active user session.
  
  - **Password Encryption:**  
    All user passwords are encrypted using a strong hashing algorithm (e.g., BCrypt). This ensures that plain-text passwords are never stored in the database, adding an extra layer of security.

### 2. **Role-Based Access Control (RBAC)**

- **User Roles:**  
  The application implements role-based access control (RBAC) to ensure that only authorized users can perform certain operations. For example:
  - **Admin** can manage all users and view all transactions.
  - **Parent** can only view their own payment transactions and student-related information.
  - **Student** can view their own balance and payment history.
  
  - **Access Control List (ACL):**  
    ACLs are implemented on the service layer to enforce restrictions based on the user’s role. Each endpoint checks the user's role to ensure they have the correct permissions for the action.

### 3. **Transaction Integrity and Isolation**

- **Transaction Management:**  
  The application uses Spring’s `@Transactional` annotation to ensure that payment processing is atomic. If any step of the payment process fails (e.g., insufficient funds, database error), all changes are rolled back to maintain the integrity of the transaction.

  - **Isolation Level:**  
    The default isolation level ensures that each transaction operates independently of others. This prevents issues like double-spending or conflicting updates during concurrent transactions.
  
  - **Error Handling:**  
    Detailed exception handling is implemented to catch and log any errors during the transaction lifecycle. Custom exceptions (like `ApiException`) are thrown when business rules are violated, such as insufficient funds or invalid user relationships.

---

## Multi-Table Payment Processing Design Decisions

### 1. **Entities Involved in Payment Processing**

The core entities involved in the payment process are:
- **Student:** Represents a student who is recieving a payment.
- **Parent:** Represents a parent or guardian making a payment on behalf of a student.
- **Payment:** Represents the payment transaction, including original amounts, adjusted amounts (with dynamic rates), and the status of the transaction.
  
These entities are mapped to multiple tables in the database:

- `student`: Contains student information and balance.
- `parent`: Contains parent information, balance, and their relationship to students.
- `payment`: Stores payment transaction records, including payment status, amounts, and associated parent and student references.

### 2. **Designing for Multiple Parents and Shared Payments**

The system supports a scenario where a student can have multiple parents, and payments can be shared between parents.

- **One Parent Payment:**  
  When a student is linked to only one parent, the entire payment amount is deducted from that parent's balance and credited to the student's balance. 
  - The payment status is marked as "SUCCESS" once the transaction is processed successfully.

- **Two Parent Payment:**  
  When a student is linked to two parents, the payment is split between the parents. The system dynamically calculates the contribution percentage based on the available balance of each parent. 
  - Contributions are calculated based on a dynamic formula that checks the available balance of each parent to determine how much each parent will contribute (e.g., 60%/40%, 80%/20%, etc.).
    
  
  - **Dynamic Rate Calculation:**  
    A dynamic rate is applied to the total amount based on factors like:
      - The original payment amount.
      - Whether the student has multiple parents.
      - Other factors like payment amount thresholds (e.g., > $1000).
  
    This ensures that the payment amount is fairly distributed between the parents based on their financial capability.

### 3. **Database Schema and Relationships**

The entities are designed to reflect the relationships between students and parents in a normalized manner.

- **many-to-Many Relationship (Student ↔ Parent):**  
  mutpile student can have multiple parents, so the `student` table has a many-to-many relationship with the `parent` table. Each parent is associated with one or more students.
    
- **(Payment ↔ Student/Parent):**  
  Each payment is associated with both a student and a parent. The `payment` .
  
  - Payments can be tracked by `payment_id`, and each payment record links a specific student and parent .

### 4. **Handling Multiple Payments with Atomicity**

The `@Transactional` annotation ensures that payments involving multiple parents are processed atomically, meaning:
- All payment deductions and credits happen within a single transaction.
- If there is an issue (e.g., insufficient funds for one parent), the entire payment is canceled, and no changes are made to the database.

## Conclusion

This payment processing system design prioritizes both security and flexibility. The use of JWT ensures secure authentication and authorization, while the multi-table design allows for efficient handling of shared payments among parents. The payment system's atomic transactions ensure that financial integrity is maintained, even in complex scenarios where payments are split across multiple parents.




# - Explanation of Arithmetic Logic and Balance Updates

## Overview

In the payment processing system, the arithmetic logic plays a critical role in calculating the adjusted payment amounts, distributing payments across parents (in the case of shared payments), and updating the balances of both parents and students. The system uses various dynamic rates and contributions, which are calculated based on the specific conditions of the payment, such as the total amount, number of parents, and the available balances of the parents.

### 1. **Dynamic Rate Calculation**

A **dynamic rate** is applied to the original payment amount based on various factors, such as:
- The original amount of the payment.
- Whether the student has multiple parents.
- Payment amount thresholds (e.g., if the payment is greater than $1000).
- Additional rules that are defined based on business needs.

#### Formula for Dynamic Rate Calculation

The **base dynamic rate** starts at 2% (0.02), and this rate is adjusted based on the following conditions:
1. **Payment Amount:**  
   If the payment amount is greater than $1000, an additional 2% is added to the base rate.
   - Formula:  
     `dynamicRate = baseRate + 0.02 (if amount > 1000)`

2. **Shared Students:**  
   If the student is linked to more than one parent, an additional 0.5% is added to the dynamic rate. This is to account for the extra complexity of splitting the payment between parents.
   - Formula:  
     `dynamicRate = dynamicRate + 0.005 (if student has more than one parent)`

This dynamic rate directly influences the **adjusted payment amount**, which is the amount the parents need to pay after the dynamic rate is applied.

#### Example of Dynamic Rate Calculation:
- If the original payment is $1200 and the student has multiple parents, the dynamic rate is:
  - `baseRate = 0.02`
  - `additional rate (amount > $1000) = 0.02`
  - `additional rate (shared student) = 0.005`
  - Therefore, `dynamicRate = 0.02 + 0.02 + 0.005 = 0.045 (or 4.5%)`

### 2. **Adjusted Payment Amount**

Once the **dynamic rate** is calculated, it is used to determine the **adjusted payment amount**, which is the amount that will actually be processed in the payment.

#### Formula for Adjusted Amount:
- The **adjusted amount** is calculated by multiplying the original payment amount by `1 + dynamicRate`:
  - `adjustedAmount = originalAmount * (1 + dynamicRate)`

#### Example of Adjusted Amount Calculation:
- If the original amount is $1200 and the dynamic rate is 4.5%:
  - `adjustedAmount = 1200 * (1 + 0.045) = 1200 * 1.045 = 1254`

This adjusted amount is what will be deducted from the parent(s) and credited to the student’s balance.

---

## 3. **Payment Distribution Between Parents**

When the student is linked to multiple parents, the payment amount is divided between them based on their available balances. The system ensures that each parent contributes a fair share of the payment based on their financial ability.

#### Example: Two Parent Payment

Let’s say the student has two parents, and the **adjusted amount** is $1254. The logic ensures that both parents contribute in a way that is proportionate to their available balances.

- **Parent 1** has a balance of $2000.
- **Parent 2** has a balance of $1000.

The system calculates the contribution from each parent as follows:

1. **Contribution Calculation:**
   The system first calculates the potential contribution from each parent based on their balance, and this calculation is done dynamically depending on their available funds.

2. **Contribution Percentage Logic:**
   The contribution from each parent is based on the percentage of the total balance that they control. For example:
   - If Parent 1 has 2/3 of the total available balance and Parent 2 has 1/3, then Parent 1 will contribute 2/3 of the total adjusted payment, and Parent 2 will contribute the remaining 1/3.

   - Formula for Parent Contributions:
     - `Parent1Contribution = adjustedAmount * (Parent1Balance / (Parent1Balance + Parent2Balance))`
     - `Parent2Contribution = adjustedAmount * (Parent2Balance / (Parent1Balance + Parent2Balance))`

3. **Example Calculation:**
   - `Parent1Contribution = 1254 * (2000 / (2000 + 1000)) = 1254 * (2000 / 3000) = 836`
   - `Parent2Contribution = 1254 * (1000 / (2000 + 1000)) = 1254 * (1000 / 3000) = 418`

Thus, **Parent 1** will contribute **$836** and **Parent 2** will contribute **$418**.

---

## 4. **Balance Updates**

Once the contributions are calculated, the balances of the parents and the student are updated accordingly.

### 1. **Parent Balance Update**

After calculating each parent’s contribution, their balance is deducted by the amount they are contributing to the payment.

- **Parent 1’s new balance:**
  - `Parent1NewBalance = Parent1Balance - Parent1Contribution = 2000 - 836 = 1164`

- **Parent 2’s new balance:**
  - `Parent2NewBalance = Parent2Balance - Parent2Contribution = 1000 - 418 = 582`

### 2. **Student Balance Update**

The **adjusted amount** is credited to the student’s balance. The full amount is credited to the student’s account, regardless of how it is split between the parents.

- **Student New Balance:**
  - `StudentNewBalance = StudentBalance + adjustedAmount = StudentBalance + 1254`

### 3. **Payment Success and Updates**

- Once the contributions are processed, the payment is marked as **SUCCESS** in the system, and all balances (parent and student) are updated accordingly.
- If any of the parents' balances are insufficient to cover their contributions, the entire transaction is rolled back, and an error is returned indicating **"Insufficient Funds."**

---

## 5. **Summary of Arithmetic Logic Effects**

- **Dynamic Rate:** The dynamic rate calculation adjusts the original payment amount to account for various factors like the amount of the payment and whether the student is shared.
- **Contribution Logic:** When multiple parents are involved, the payment is split based on the available balances of each parent, ensuring fairness in the contribution process.
- **Balance Updates:** The balances of parents are decremented by their contributions, while the student's balance is incremented by the total adjusted amount.

This arithmetic logic ensures that the payment processing is both fair and accurate, with proper handling of complex scenarios such as shared payments between parents and dynamic adjustments based on payment conditions.





## 🛠️ Getting Started

## 📋 Prerequisites

Ensure the following tools are installed on your machine before running the application:

- **Java 11** or newer (required for building and running the Spring Boot application)
- **Maven** (for dependency management and building)
- **PostgreSQL** (for database)
- **Git** (to clone the repository)

### Install Java 11
- Download and install from [Oracle’s official website](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) or use your preferred package manager.

### Install Maven
- Download Maven from [Maven's official website](https://maven.apache.org/download.cgi) or use package managers like `apt` (Linux), `brew` (Mac), etc.

### Install PostgreSQL
- Install PostgreSQL from [PostgreSQL’s official website](https://www.postgresql.org/download/).

---
## ⚙️ Build and Run

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/payment-processing-system.git
cd payment-processing-system
