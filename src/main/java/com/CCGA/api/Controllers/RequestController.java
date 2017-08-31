package com.CCGA.api.Controllers;

import com.CCGA.api.Models.*;
import com.CCGA.api.Repositorys.BookRepo;
import com.CCGA.api.Repositorys.MajorRepo;
import com.CCGA.api.Repositorys.RequestRepo;
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
@RequestMapping("/api/request")
public class RequestController {

    private RequestRepo requests;
    private UserRepo users;
    private BookRepo books;
    private MajorRepo majors;

    @Autowired
    public RequestController(RequestRepo requests, UserRepo users, BookRepo books, MajorRepo majors) {
        this.requests = requests;
        this.users = users;
        this.books = books;
        this.majors = majors;
    }

    //Get a list of all requests
    @GetMapping("/all")
    public ResponseEntity getAllRequests(HttpSession session) {
        if (session.getAttribute("userID") != null) {
            return ResponseEntity.status(OK).body(new JSONResponse("Success", requests.findAll()));
        } else {
            return ResponseEntity.status(UNAUTHORIZED).body(new JSONResponse("You must be logged in to do that", null));
        }
    }

    //Get a specific request
    @GetMapping("/{requestID}")
    public ResponseEntity getASpecificRequest(@PathVariable int requestID, HttpSession session) {
        if (session.getAttribute("userID") != null) {
            Request foundRequest = requests.findOne(requestID);
            try {
                requireNonNull(foundRequest);
                return ResponseEntity.status(OK).body(new JSONResponse("Success", foundRequest));
            } catch (NullPointerException ex) {
                return ResponseEntity.status(NOT_FOUND).body(new JSONResponse("Request with that ID not found", null));
            }
        } else {
            return ResponseEntity.status(UNAUTHORIZED).body(new JSONResponse("You must be logged in to do that", null));
        }
    }

    //Get a list of all requests a logged in user made
    @GetMapping("/mine/all")
    public ResponseEntity getAllRequestMadeByLoggedIn(HttpSession session) {
        if (session.getAttribute("userID") != null) {
            User loggedIn = users.findOne((int) session.getAttribute("userID"));
            List<Request> requestListByUser = new ArrayList<>();

            requestListByUser.addAll(requests.findAllByUserRequestedEquals(loggedIn));
            return ResponseEntity.status(OK).body(new JSONResponse("Success", requestListByUser));
        } else {
            return ResponseEntity.status(UNAUTHORIZED).body(new JSONResponse("You must be logged in to do that", null));
        }
    }

    //Get a specific request a logged in user made
    @GetMapping("/mine/{requestID}")
    public ResponseEntity getASpecificRequestByLoggedIn(@PathVariable int requestID, HttpSession session) {
        if (session.getAttribute("userID") != null) {

            User loggedIn = users.findOne((int) session.getAttribute("userID"));
            Request foundRequest = requests.findByRequestIDAndUserRequested(requestID, loggedIn);

            if (foundRequest != null) {
                return ResponseEntity.status(OK).body(new JSONResponse("Success", foundRequest));
            } else {
                return ResponseEntity.status(NOT_FOUND).body(new JSONResponse("Request with that ID not found by logged in user", null));
            }
        } else {
            return ResponseEntity.status(UNAUTHORIZED).body(new JSONResponse("You must be logged in to do that", null));
        }
    }

    //Create a new request for a book from logged in user
    @PostMapping(value = "/create", consumes = "application/json")
    public ResponseEntity createBookRequest(@RequestBody String bookRequested, HttpSession session) {
        if (session.getAttribute("userID") != null) {
            User loggedIn = users.findOne((int) session.getAttribute("userID"));
            JsonNode json;

            try {
                json = new ObjectMapper().readTree(new StringReader(bookRequested));
                if (json == null) {
                    return ResponseEntity.status(BAD_REQUEST).body(new JSONResponse("No data supplied", null));
                }
            } catch (IOException e) {
                return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new JSONResponse("Error processing request, please try again", null));
            }

            Book requested = new Book();

            if (json.has("bookID")) {
                requested = books.findOne(json.get("bookID").asInt());
                Request newRequest = new Request(requested, loggedIn);
                requests.save(newRequest);
                return ResponseEntity.status(CREATED).body(new JSONResponse("Request created", newRequest));

            } else {
                if (json.has("author") && json.has("isbn") && json.has("name")) {
                    requested.setName(json.get("name").asText());
                    requested.setAuthor(json.get("author").asText());
                    requested.setIsbn(json.get("isbn").asText());
                    requested.setMajor(majors.findOne(json.get("majorID").asInt()));
                    Request newRequest = new Request(requested, loggedIn);
                    books.save(requested);
                    requests.save(newRequest);
                    return ResponseEntity.status(CREATED).body(new JSONResponse("Request created", newRequest));

                } else {
                    return ResponseEntity.status(BAD_REQUEST).body(new JSONResponse("Please supply an ISBN number to search for, or a \"name\", \"author\", \"isbn\", and \"majorID\" to create a new" +
                        " book", null));
                }
            }

        } else {
            return ResponseEntity.status(UNAUTHORIZED).body(new JSONResponse("You must be logged in to do that", null));
        }
    }

    //Create a new request for a book from logged in user
    @PostMapping(value = "/create", consumes = "application/x-www-form-urlencoded;charset=UTF-8")
    public ResponseEntity createBookRequestFormData(Integer bookID, String name, String author, String isbn, Integer majorID, HttpSession session) {
        if (session.getAttribute("userID") != null) {
            User loggedIn = users.findOne((int) session.getAttribute("userID"));

            Book requested;
            if (bookID != null) {
                requested = books.findOne(bookID);

                Request newRequest = new Request(requested, loggedIn);
                requests.save(newRequest);

                return ResponseEntity.status(CREATED).body(new JSONResponse("Request created", newRequest));
            } else /*No ISBN provided*/ {
                if (name != null && author != null && isbn != null && majorID != null) {
                    Major major = majors.findOne(majorID);
                    if (major != null) {
                        requested = new Book();
                        requested.setName(name);
                        requested.setAuthor(author);
                        requested.setIsbn(isbn);
                        requested.setMajor(major);
                        Request newRequest = new Request(requested, loggedIn);

                        requests.save(newRequest);
                        books.save(requested);

                        return ResponseEntity.status(CREATED).body(new JSONResponse("Request created", newRequest));
                    } else {
                        return ResponseEntity.status(BAD_REQUEST).body(new JSONResponse("Major with provided majorID not found.", null));
                    }
                } else {
                    return ResponseEntity.status(BAD_REQUEST).body(new JSONResponse("Please supply an ISBN number to search for, or a \"name\", \"author\", \"isbn\", and \"majorID\" to create a new" +
                        " book", null));
                }
            }

        } else {
            return ResponseEntity.status(UNAUTHORIZED).body(new JSONResponse("You must be logged in to do that", null));
        }
    }

    @PostMapping("/create")
    public ResponseEntity createBookRequestMediaNotSupported(HttpSession session) {
        if (session.getAttribute("userID") != null) {
            return ResponseEntity.status(UNSUPPORTED_MEDIA_TYPE).body(new JSONResponse("Content-Type not supported, please use \"application/json\"", null));
        } else {
            return ResponseEntity.status(UNAUTHORIZED).body(new JSONResponse("You must be logged in to do that", null));
        }
    }

    //Delete a request a logged in user made
    @DeleteMapping("/delete/{requestID}")
    public ResponseEntity deleteARequest(@PathVariable int requestID, HttpSession session) {
        if (session.getAttribute("userID") != null) {

            User loggedIn = users.findOne((int) session.getAttribute("userID"));
            Request foundRequest = requests.findByRequestIDAndUserRequested(requestID, loggedIn);

            if (foundRequest != null) {
                requests.delete(foundRequest);
                return ResponseEntity.status(NO_CONTENT).build();
            } else {
                return ResponseEntity.status(NOT_FOUND).body(new JSONResponse("Request with that ID not found by logged in user", null));
            }
        } else {
            return ResponseEntity.status(UNAUTHORIZED).body(new JSONResponse("You must be logged in to do that", null));
        }
    }
}