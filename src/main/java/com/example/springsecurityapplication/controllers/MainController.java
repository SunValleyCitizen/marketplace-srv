package com.example.springsecurityapplication.controllers;

import com.example.springsecurityapplication.enumm.Role;
import com.example.springsecurityapplication.enumm.Status;
import com.example.springsecurityapplication.models.Cart;
import com.example.springsecurityapplication.models.Order;
import com.example.springsecurityapplication.models.Person;
import com.example.springsecurityapplication.models.Product;
import com.example.springsecurityapplication.repositories.CartRepository;
import com.example.springsecurityapplication.repositories.OrderRepository;
import com.example.springsecurityapplication.repositories.ProductRepository;
import com.example.springsecurityapplication.security.PersonDetails;
import com.example.springsecurityapplication.services.OrderService;
import com.example.springsecurityapplication.services.PersonService;
import com.example.springsecurityapplication.services.ProductService;
import com.example.springsecurityapplication.util.PersonValidator;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class MainController {

    private final PersonValidator personValidator;
    private final PersonService personService;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final OrderService orderService;

    public MainController(PersonValidator personValidator, PersonService personService, ProductService productService,
            ProductRepository productRepository, CartRepository cartRepository, OrderService orderService) {
        this.personValidator = personValidator;
        this.personService = personService;
        this.productService = productService;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.orderService = orderService;
    }

    @GetMapping("/person_account")
    public String index(Model model) {
        // Получаем объект аутентификации --> с помощью SpringContextHolder обращаемся к
        // контексту и на нем вызываем метод аутентификации. Из сессии текущего
        // пользователя получаем объект, который был положен в данную сессию после
        // аутентификации пользователя
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        Role role = personDetails.getPerson().getRole();
        if (role.equals(Role.ROLE_ADMIN)) {
            return "redirect:/admin";
        }
        // System.out.println(personDetails.getPerson());
        // System.out.println("ID пользователя: " + personDetails.getPerson().getId());
        // System.out.println("Логин пользователя: " +
        // personDetails.getPerson().getLogin());
        // System.out.println("Пароль пользователя: " +
        // personDetails.getPerson().getPassword());
        model.addAttribute("products", productService.getAllProduct());
        return "/product/product";
    }

    @GetMapping("/")
    public String mainPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ANONYMOUS"));
        if (isAnonymous == true) {
            return "redirect:/product";
        }
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        Role role = personDetails.getPerson().getRole();
        if (role.equals(Role.ROLE_ADMIN)) {
            return "redirect:/product";
        } else if (role.equals(Role.ROLE_USER)) {
            return "redirect:/person_account";
        }
        return "redirect:/product";
    }

    @GetMapping("/registration")
    public String registration(@ModelAttribute("person") Person person) {
        return "registration";
    }

    @PostMapping("/registration")
    public String resultRegistration(@ModelAttribute("person") @Valid Person person, BindingResult bindingResult) {
        personValidator.validate(person, bindingResult);
        if (bindingResult.hasErrors()) {
            return "registration";
        }
        personService.register(person);
        return "redirect:/person_account";
    }

    @GetMapping("/person_account/product/info/{id}")
    public String infoProduct(@PathVariable("id") int id, Model model) {
        model.addAttribute("product", productService.getProductId(id));
        return "/user/infoProduct"; 
    }

    @GetMapping("/cart/add/{id}")
    public String addProductInCart(@PathVariable("id") int id, Model model) {
        // Получаем продукт по id
        Product product = productService.getProductId(id);
        // Извлекаем объект аутентифицированного пользователя
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        // Извлекаем id пользователя из объекта
        int id_person = personDetails.getPerson().getId();
        Cart cart = new Cart(id_person, product.getId());
        cartRepository.save(cart);
        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String cart(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        // Извлекаем id пользователя из объекта
        int id_person = personDetails.getPerson().getId();

        List<Cart> cartList = cartRepository.findByPersonId(id_person);
        List<Product> productList = new ArrayList<>();

        // Получаем продукты из корзины по id товара
        for (Cart cart : cartList) {
            productList.add(productService.getProductId(cart.getProductId()));
        }

        // Вычисление итоговой цена
        float price = 0;
        for (Product product : productList) {
            price += product.getPrice();
        }

        model.addAttribute("price", price);
        model.addAttribute("cart_product", productList);
        return "/user/cart";
    }

    @GetMapping("/cart/delete/{id}")
    public String deleteProductFromCart(Model model, @PathVariable("id") int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        // Извлекаем id пользователя из объекта
        int id_person = personDetails.getPerson().getId();
        List<Cart> cartList = cartRepository.findByPersonId(id_person);
        List<Product> productList = new ArrayList<>();

        // Получаем продукты из корзины по id товара
        for (Cart cart : cartList) {
            productList.add(productService.getProductId(cart.getProductId()));
        }
        cartRepository.deleteCartByProductId(id);
        return "redirect:/cart";
    }

    @GetMapping("/order/create")
    public String order() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        // Извлекаем id пользователя из объекта
        int id_person = personDetails.getPerson().getId();

        List<Cart> cartList = cartRepository.findByPersonId(id_person);
        List<Product> productList = new ArrayList<>();

        // Получаем продукты из корзины по id товара
        for (Cart cart : cartList) {
            productList.add(productService.getProductId(cart.getProductId()));
        }

        // Вычисление итоговой цена
        float price = 0;
        for (Product product : productList) {
            price += product.getPrice();
        }

        String uuid = UUID.randomUUID().toString();
        for (Product product : productList) {
            Order newOrder = new Order(
                    uuid, product, personDetails.getPerson(), 1, product.getPrice(), Status.Оформлен);
            orderService.saveOrder(newOrder);
            cartRepository.deleteCartByProductId(product.getId());
        }
        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String orderUser(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        List<Order> orderList = orderService.findByPerson(personDetails.getPerson());
        model.addAttribute("orders", orderList);
        return "/user/orders";
    }
}