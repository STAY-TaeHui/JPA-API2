package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


/* Collection이 아닌 Entity들의 연관관계 (XToOne)
* Order
* Order -> Member
* Order -> Delivery
* */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {
    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1(){
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        return all;
    }

    /*
    * Order 하나를 가져오기 위해서
    * SQL 1번에 결과 2개 (Member 2번(userA, userB), Delivery 2번)
    * 1+N 문제!!!
    * */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2(){
        return orderRepository.findAllByString(new OrderSearch()).stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());
    }

    /*
    * fetch join으로 쿼리 1번으로 조회
    * */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3(){
        List<Order> orders = orderRepository.findaAllWithMemberDelivery();
        return orders.stream()
                .map(order -> new SimpleOrderDto(order))
                .collect(Collectors.toList());
    }

    /*
    * 위의 v3보다 성능이 조금 더 향상. -> 성능테스트 해보길.
    * JPQL 결과를 DTO로 바로 변환
    * Entity를 로직에서 사용할 수 없음.
    * 재사용성이 떨어짐 -> 해당 DTO에 의존적인 코드.
    * Repository에서도 DTO에 의존됨.-> Repository에서는 Entity조회하는 용도이다.
    *
    * 이럴때는 SimpleQuery 별도의 Repository를 별도로 뽑아서 구현한다.
    *
    * 쿼리 방식 선택 권장순서
    * 1. v2 방식
    * 2. v3
    * 3. v4
    * 4. 최후로는 네이티브 SQL이나 JDBC Template 를 활용하여 직접 SQL을 사용함.
    * */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4(){
//        return orderRepository.findOrderDtos();
        return orderSimpleQueryRepository.findOrderDtos(); //화면에 맞춰진
    }


    @Data
    static class SimpleOrderDto{
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order){
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
        }
    }

}
