package com.C_platform.controller;

import com.C_platform.item.entity.Item;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/items")
public class SwaggerTestController {

        // 단순 테스트용
        @GetMapping("/{id}")
        public ResponseEntity<Item> getItem(@PathVariable int id) {
            Item item = new Item();
            item.setId(id);
            item.setItemName("테스트 상품");
            item.setItemPrice(10000);
            return ResponseEntity.ok(item);
        }

        @PostMapping
        public ResponseEntity<Item> createItem(@RequestBody Item item) {
            item.setId(1); // 더미 값
            return ResponseEntity.ok(item);
        }
}
