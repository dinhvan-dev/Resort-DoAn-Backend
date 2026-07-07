package com.example.resort.repository;

import com.example.resort.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InValidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {
    // JpaRepository: Đây là interface cốt lõi cung cấp các phương thức CRUD cơ bản
    //  InValidateTokenRepository: Tên repository cho entity InValidateToken
    // extends JpaRepository<InValidateToken, String>: kế thừa từ JpaRepository với :
    // InValidateToken: Entity type mà repository quản lý
    // String: kiểu dữ liệu của Id( JWT ID là String)

    // CRUD cơ bản
    // save( S emtity) : lưu hoặc cập nhật entity
    // saveAll(Interable<S> entities) : lưu nhiều entities ( thực thể )
    // findById(Id id) : tìm theo Id
    // existsById(ID id) : kiểm tra tồn tại theo Id
    // findAll() : lấy tất cả records ( hồ sơ )
    // findAllById(Interable<Id> ids) : tìm nhiều records theo Ids
    // count() : đếm số lượng records
    // deleteById(Id id) : xóa theo Id
    // delete(T entity) : xóa theo entity
    // deleteAll() : xóa tất cả

    // Phân trang sắp xếp
    // findAll(Pageable pageable) : lấy records có phân trang ( pageable = có thể phân trang )
    // findAll(Sort sort): lấy records có sắp xếp ( sort = loại)
}
