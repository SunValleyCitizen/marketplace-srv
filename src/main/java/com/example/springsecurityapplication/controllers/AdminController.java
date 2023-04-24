package com.example.springsecurityapplication.controllers;

import com.example.springsecurityapplication.enumm.Role;
import com.example.springsecurityapplication.enumm.Status;
import com.example.springsecurityapplication.models.Category;
import com.example.springsecurityapplication.models.Image;
import com.example.springsecurityapplication.models.Order;
import com.example.springsecurityapplication.models.Person;
import com.example.springsecurityapplication.models.Product;
import com.example.springsecurityapplication.repositories.CategoryRepository;
import com.example.springsecurityapplication.security.PersonDetails;
import com.example.springsecurityapplication.services.OrderService;
import com.example.springsecurityapplication.services.PersonService;
import com.example.springsecurityapplication.services.ProductService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;
    private final PersonService personService;

    @Value("${upload.path}")
    private String uploadPath;

    private final CategoryRepository categoryRepository;

    public AdminController(ProductService productService, OrderService orderService, PersonService personService,
            CategoryRepository categoryRepository) {
        this.productService = productService;
        this.orderService = orderService;
        this.personService = personService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("admin/product/add")
    public String addProduct(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("category", categoryRepository.findAll());
        return "product/addProduct";
    }

    @PostMapping("/admin/product/add")
    public String addProduct(@ModelAttribute("product") @Valid Product product, BindingResult bindingResult,
            @RequestParam("file_one") MultipartFile file_one, @RequestParam("file_two") MultipartFile file_two,
            @RequestParam("file_three") MultipartFile file_three, @RequestParam("file_four") MultipartFile file_four,
            @RequestParam("file_five") MultipartFile file_five, @RequestParam("category") int category, Model model)
            throws IOException {
        Category category_db = (Category) categoryRepository.findById(category).orElseThrow();
        System.out.println(category_db.getName());
        if (bindingResult.hasErrors()) {
            model.addAttribute("category", categoryRepository.findAll());
            return "product/addProduct";
        }
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdir();
        }
        MultipartFile[] files = { file_one, file_two, file_three, file_four, file_five };
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String uuidFile = UUID.randomUUID().toString();
            String resultFileName = uuidFile + "." + file.getOriginalFilename();
            file.transferTo(new File(uploadPath + "/" + resultFileName));
            Image image = new Image();
            image.setProduct(product);
            image.setFileName(resultFileName);
            product.addImageToProduct(image);
        }
        productService.saveProduct(product, category_db);

        return "redirect:/admin";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("products", productService.getAllProduct());
        return "admin/index";
    }

    @GetMapping("admin/product/delete/{id}")
    public String deleteProduct(@PathVariable("id") int id) {
        productService.deleteProduct(id);
        return "redirect:/admin";
    }

    @GetMapping("admin/product/edit/{id}")
    public String editProduct(Model model, @PathVariable("id") int id) {
        model.addAttribute("product", productService.getProductId(id));
        model.addAttribute("category", categoryRepository.findAll());
        return "product/editProduct";
    }

    @PostMapping("admin/product/edit/{id}")
    public String editProduct(@ModelAttribute("product") @Valid Product product, BindingResult bindingResult,
            @PathVariable("id") int id, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("category", categoryRepository.findAll());
            return "product/editProduct";
        }
        productService.updateProduct(id, product);
        return "redirect:/admin";
    }

    @GetMapping("admin/orders")
    public String adminOrders(Model model) {
        List<Order> allOrders = orderService.getAllOrders();
        model.addAttribute("orders", allOrders);

        Status[] statuses = Status.values();
        model.addAttribute("statuses", statuses);
        return "admin/orders";
    }

    @PostMapping("/admin/orders/search")
    public String orderSearch(@RequestParam("search") String search, Model model) {
        List<Order> allOrders = orderService.getAllOrders();
        List<Order> filteredOrders = allOrders.stream().filter(order -> order.getNumber().endsWith(search))
                .collect(Collectors.toList());
        model.addAttribute("orders", filteredOrders);

        Status[] statuses = Status.values();
        model.addAttribute("statuses", statuses);
        return "admin/orders";
    }

    @PostMapping("admin/order/edit/{id}")
    public String editOrder(@PathVariable("id") int id, @RequestParam("status") String statusString, Model model) {
        Status status = Status.valueOf(statusString);
        Optional<Order> orderOptional = orderService.getAllOrders().stream().filter(ord -> ord.getId() == id)
                .findFirst();
        if (orderOptional.isPresent() && status != null) {
            Order order = orderOptional.get();
            order.setStatus(status);
            orderService.updateOrder(id, order);
        }
        return "redirect:/admin/orders";
    }

    @GetMapping("admin/order/delete/{id}")
    public String deleteOrder(@PathVariable("id") int id) {
        orderService.deleteOrder(id);
        return "redirect:/admin/orders";
    }

    @GetMapping("admin/users")
    public String adminUsers(Model model) {
        List<PersonDetails> allPersonDetails = personService.getAllPersons().stream().map(PersonDetails::new)
                .collect(Collectors.toList());
        allPersonDetails.sort((usr1, user2) -> usr1.getPerson().getId() - user2.getPerson().getId());
        model.addAttribute("allPersonDetails", allPersonDetails);

        Role[] roles = Role.values();
        model.addAttribute("roles", roles);
        return "admin/users";
    }

    @PostMapping("admin/user/edit/{id}")
    public String editPerson(@PathVariable("id") int id, @RequestParam("role") String roleString, Model model, HttpServletRequest request) {
        Role role = Role.valueOf(roleString);
        Optional<Person> personOptional = personService.getAllPersons().stream().filter(usr -> usr.getId() == id)
                .findFirst();
        if (personOptional.isPresent() && role != null) {
            Person person = personOptional.get();
            person.setRole(role);
            personService.updatePerson(id, person);

            // перенаправить пользователя на logout если он сменил свою роль на отличную от
            // ROLE_ADMIN
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
            int loggedInPersonId = personDetails.getPerson().getId();
            if (loggedInPersonId == id && role != Role.ROLE_ADMIN) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                return "redirect:/";
            }
        }
        return "redirect:/admin/users";
    }
}
