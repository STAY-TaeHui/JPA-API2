package jpabook.jpashop.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController
{
    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

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
    /*
    * 컬렉션은 페치 조인시 페이징이 불가능
    * ToOne 관계는 페치 조인으로 쿼리 수 최적화
    * 컬렉션은 페치조인 댓니에 지연로딩을 유지하고 @BatchSize 로 최적화 -> IN 절 */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value = "offset",defaultValue = "0") int offset,
                                        @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);
        List<OrderDto> result = orders.stream()
            .map(o -> new OrderDto(o))
            .collect(toList());
        return result;
    }

    /*
    * JPA에서 DTO를 직접 조회
    * */
   @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4(){
        return orderQueryRepository.findOrderQueryDtos();
    }

    /*
    * 컬렉션 조회 최적화 - 데이터가 뻥튀기 될때
    * IN절을 활용해서 메모리에 미리 조회해서 반복문 돌리면서 최적화
    * --> 하지만 Entity로 조회하게 되면 @BatchSize로 가능하다.
    * */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5(){
        return orderQueryRepository.findAllByDto_optimization();
    }

    /*
    * 플랫 데이터 최적화
    * JOIN결과를 그대로 조회 후 Application 단에서 원하는 모양으로 변환*/
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6(){
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();
        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(),
                        o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(),o.getCount()), toList())))
                .entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(),
                        e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                        e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }


    /*  --- 정리 ---
    * 권장 순서
    * 1. 엔티티 조회 방식으로 우선 접근
    *   1. 페치조인으로 쿼리수 최적화
    *   2. 컬렉션 최적화
    *       1. 페이징 필요 -> @BatchSize 로 최적화
    *       2. 페이징 필요 X -> 페치 조인 사용
    * 2. 엔티티 조회방식으로 안되면 DTO로 직접 조회
    * 3. DTO 직접조회로 해결이 안되면 NativeSQL
    *
    * 캐시가 필요할 땐 Entity를 캐싱하지 말고 DTO를 캐싱!!
    * */
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
