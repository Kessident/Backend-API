package com.CCGA.api.Controllers;

import com.CCGA.api.Models.Book;
import com.CCGA.api.Models.JSONResponse;
import com.CCGA.api.Models.Transaction;
import com.CCGA.api.Models.User;
import com.CCGA.api.Repositorys.BookRepo;
import com.CCGA.api.Repositorys.TransactionRepo;
import com.CCGA.api.Repositorys.UserRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/transaction")
public class TransactionController {

    private TransactionRepo transactions;
    private UserRepo users;
    private BookRepo books;

    @Autowired
    public TransactionController(TransactionRepo transactions, UserRepo users, BookRepo books) {
        this.transactions = transactions;
        this.users = users;
        this.books = books;
    }

    @GetMapping("/all")
    public ResponseEntity getAllTransactions() {
        List<Transaction> allTransactions = new ArrayList<>();
        transactions.findAll().forEach(allTransactions::add);
        return ResponseEntity.status(OK).body(new JSONResponse("success", allTransactions));
    }

    @GetMapping("/{transID}")
    public ResponseEntity getSpecificTransaction(@PathVariable int transID) {
        Transaction transaction = transactions.findOne(transID);
        return ResponseEntity.status(OK).body(new JSONResponse("success", transaction));
    }

    @PostMapping(value = "/create", consumes = "application/json")
    public ResponseEntity createNewTransaction(@RequestBody String transactionAsString, HttpSession session) {
        if (session.getAttribute("userID") != null) {

            JsonNode transactionAsJson;

            try {
                transactionAsJson = new ObjectMapper().readTree(new StringReader(transactionAsString));
                if (transactionAsJson == null) {
                    throw new IOException();
                }
            } catch (IOException ex) {
                return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new JSONResponse("Error", "Error Processing JSON request"));
            }

            Transaction newTrans = new Transaction();

            try {
                User seller = users.findOne(transactionAsJson.get("sellerID").asInt());
                User buyer = users.findOne(transactionAsJson.get("buyerID").asInt());
                Book bookSold = books.findOne(transactionAsJson.get("bookSold").asInt());
                newTrans.setSeller(seller);
                newTrans.setBuyer(buyer);
                newTrans.setBookSold(bookSold);
                newTrans.setAmountSoldFor(transactionAsJson.get("amountSoldFor").asLong());
            } catch (Exception e) {
                return ResponseEntity.status(BAD_REQUEST).body(new JSONResponse("Error", "Error reading properties from JSON, Ensure all fields are properly spelled/present and try again"));
            }

            transactions.save(newTrans);

            return ResponseEntity.status(CREATED).body(new JSONResponse("Transaction created", newTrans));
        } else {
            return ResponseEntity.status(UNAUTHORIZED).body(new JSONResponse("You must be logged in to do that", null));
        }
    }

    @PostMapping(value = "/create", consumes = "application/x-www-form-urlencoded;charset=UTF-8")
    public ResponseEntity createNewTransactionFormData(Integer sellerID, Integer buyerID, Integer bookSoldID, Long amountSoldFor, HttpSession session) {
        if (session.getAttribute("userID") != null) {
            try {
                requireNonNull(sellerID);
                requireNonNull(buyerID);
                requireNonNull(bookSoldID);
                requireNonNull(amountSoldFor);
            } catch (NullPointerException ex) {
                return ResponseEntity.status(BAD_REQUEST).body(new JSONResponse("Please supply all required fields(sellerID, buyerID, bookSoldID, amountSoldFor)", null));
            }
            User seller = users.findOne(sellerID);
            User buyer = users.findOne(buyerID);
            Book bookSold = books.findOne(bookSoldID);

            if (seller == null) {
                return ResponseEntity.status(BAD_REQUEST).body(new JSONResponse("User with that ID not found", null));
            } else if (buyer == null) {
                return ResponseEntity.status(BAD_REQUEST).body(new JSONResponse("User with that ID not found", null));
            } else if (bookSold == null) {
                return ResponseEntity.status(BAD_REQUEST).body(new JSONResponse("Book with that ID not found", null));
            }

            Transaction newTrans = new Transaction();
            newTrans.setSeller(seller);
            newTrans.setBuyer(buyer);
            newTrans.setBookSold(bookSold);
            newTrans.setAmountSoldFor(amountSoldFor);

            transactions.save(newTrans);
            return ResponseEntity.status(CREATED).body(new JSONResponse("Transaction created", newTrans));
        } else {
            return ResponseEntity.status(UNAUTHORIZED).body(new JSONResponse("You must be logged in to do that", null));
        }
    }

    @PostMapping("/create")
    public ResponseEntity createTransactionMediaNotSupported(HttpSession session) {
        if (session.getAttribute("userID") != null) {
            return ResponseEntity.status(UNSUPPORTED_MEDIA_TYPE).body(new JSONResponse("Content-Type not supported, please use \"application/json\" or \"application/x-www-form-urlencoded\"", null));
        } else {
            return ResponseEntity.status(UNAUTHORIZED).body(new JSONResponse("You must be logged in to do that", null));
        }
    }

}
