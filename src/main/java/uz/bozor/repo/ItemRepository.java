package uz.bozor.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.bozor.entity.Item;
import uz.bozor.entity.ItemStatus;
import uz.bozor.entity.Role;
import uz.bozor.entity.User;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findBySellerAndStatusNotOrderByCreatedAtDesc(User seller, ItemStatus status);
    List<Item> findByStatusOrderByCreatedAtAsc(ItemStatus status);
    long countByStatus(ItemStatus status);
    void deleteBySeller(User seller);

    @Query("select i from Item i where i.status = :st and i.seller.role = :role order by i.createdAt desc")
    List<Item> feed(@Param("st") ItemStatus st, @Param("role") Role role, Pageable p);

    @Query(value = "select i.* from items i join users u on u.id = i.seller_id where i.status = 'ACTIVE' and u.role = 'OWNER' order by random() limit :size", nativeQuery = true)
    List<Item> random(@Param("size") int size);

    @Query("select i from Item i where i.status = :st and (lower(i.name) like lower(concat('%', :q, '%')) or lower(i.category) like lower(concat('%', :q, '%'))) order by i.createdAt desc")
    List<Item> searchByItem(@Param("st") ItemStatus st, @Param("q") String q);

    @Query("select i from Item i where i.status = :st and lower(i.seller.nickname) like lower(concat('%', :q, '%')) order by i.createdAt desc")
    List<Item> searchBySeller(@Param("st") ItemStatus st, @Param("q") String q);
}
