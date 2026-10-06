package uz.bozor.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.bozor.entity.Item;
import uz.bozor.entity.ItemStatus;
import uz.bozor.entity.User;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByStatusOrderByCreatedAtDesc(ItemStatus status, Pageable p);
    List<Item> findBySellerAndStatusNotOrderByCreatedAtDesc(User seller, ItemStatus status);
    long countByStatus(ItemStatus status);
    void deleteBySeller(User seller);

    @Query(value = "select * from items where status = 'ACTIVE' order by random() limit :size", nativeQuery = true)
    List<Item> random(@Param("size") int size);

    @Query("select i from Item i where i.status = :st and (lower(i.name) like lower(concat('%', :q, '%')) or lower(i.category) like lower(concat('%', :q, '%'))) order by i.createdAt desc")
    List<Item> searchByItem(@Param("st") ItemStatus st, @Param("q") String q);

    @Query("select i from Item i where i.status = :st and lower(i.seller.nickname) like lower(concat('%', :q, '%')) order by i.createdAt desc")
    List<Item> searchBySeller(@Param("st") ItemStatus st, @Param("q") String q);
}
