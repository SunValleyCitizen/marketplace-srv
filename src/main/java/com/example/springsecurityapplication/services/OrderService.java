package com.example.springsecurityapplication.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springsecurityapplication.models.Order;
import com.example.springsecurityapplication.models.Person;
import com.example.springsecurityapplication.repositories.OrderRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OrderService {

  private final OrderRepository orderRepository;

  public OrderService(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  // Get all orders
  public List<Order> getAllOrders() {
    List<Order> orders = orderRepository.findAll();
    Collections.sort(orders);
    return orders;
  }

  // Get all orders
  public List<Order> findByPerson(Person person) {
    List<Order> orders = orderRepository.findByPerson(person);
    Collections.sort(orders);
    return orders;
  }

  public Order getOrderId(int id) {
    Optional<Order> optionalOrder = orderRepository.findById(id);
    return optionalOrder.orElse(null);
  }

  @Transactional
  public void saveOrder(Order order) {
    orderRepository.save(order);
  }

  @Transactional
  public void updateOrder(int id, Order order) {
    order.setId(id);
    orderRepository.save(order);
  }

  @Transactional
  public void deleteOrder(int id) {
    orderRepository.deleteById(id);
  }

}
