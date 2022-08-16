package jpabook.jpashop.api;

import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.util.List;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderApiController
{
    private final OrderRepository orderRepository;

    /* V2
    XToMany 관계 Entity를 가져오는 상황
    Fetch Join을 사용하지 않고 조회했을때, 연관관계 되어있는 모든것들이 Lazy로 먹혀
    모든 Entity마다 Select를 한번씩 하게 됨. --> 수많은 쿼리문이 나간다.
    * */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2(){
        List<Order> all = orderRepository.findAll();
        List<OrderDto> result = all.stream().map(OrderDto::new)
            .collect(toList());
        return result;
    }

    /* V3
    XToMany 관계들을 Fetch Join 하여 Entity를 가져오는 상황.
    한방 쿼리로 한번에 연관되어 있는 Entity들을 엮어서 가져온다.

    이 때 distinct를 꼭 사용해야 한다!

    distinct 를 사용하는 이유는
    XToMany collection 을 가져오면서 해당 Entity(밑에서는 Order)가 Collection의 크기만큼 뻥튀기 된다.
    총 row 수 = Eneity * Collection.length
    따라서 중복되는 Entity를 제거하고 원하는 row수 만큼을 가져오기 위해서는
    distinct를 꼭 써야 한다. --> Application 단에서 중복 제거 처리
    참고 : 컬렉션 fetch join은 1개만 사용할 수 있다.
            페이징이 불가능 하다.
    * */
    @GetMapping("/api/v3/orders")
    public List<Order> ordersV3(){
        List<Order> allWithItem = orderRepository.findAllWithItem2();
        return allWithItem;
    }
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value = "offset",defaultValue = "0") int offset,
                                        @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);
        List<OrderDto> result = orders.stream()
            .map(o -> new OrderDto(o))
            .collect(toList());
        return result;
    } 


    @Data
    private static class OrderDto
    {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate; //주문시간
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;
        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                .map(orderItem -> new OrderItemDto(orderItem))
                .collect(toList());
        }

        @Data
        private class OrderItemDto
        {
            private String itemName;//상품 명
            private int orderPrice; //주문 가격
            private int count; //주문 수량
            public OrderItemDto(OrderItem orderItem) {
                itemName = orderItem.getItem().getName();
                orderPrice = orderItem.getOrderPrice();
                count = orderItem.getCount();
            }
        }
    }
}
