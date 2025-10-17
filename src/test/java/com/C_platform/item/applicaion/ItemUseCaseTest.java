package com.C_platform.item.applicaion;

import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.Member.loginType;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import com.C_platform.item.application.ItemUseCase;
import com.C_platform.item.domain.Category;
import com.C_platform.item.domain.Item;
import com.C_platform.item.infrastructure.CategoryRepository;
import com.C_platform.item.ui.dto.CreateItemRequestDto;
import com.C_platform.item.ui.dto.ItemDetailResponseDto;
import com.C_platform.item.ui.dto.Sizes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ItemUseCaseTest {

    @Autowired
    private ItemUseCase itemUseCase;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Member testMember;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Create and save a test member
        Member member = new Member();
        memberRepository.save(member);

        // Create and save a test category
        testCategory = Category.builder()
                .name("상의")
                .build();
        categoryRepository.save(testCategory);
    }

    @Test
    @DisplayName("상품을 저장하고 다시 조회했을 때 데이터가 일치해야 한다.")
    void createAndFindItemTest() {
        // given
        String itemName = "Test Jacket";
        int price = 120000;
        String description = "This is a test jacket.";
        boolean directTrade = true;

        Sizes sizes = Sizes.builder()
                .chest(55)
                .shoulder(48)
                .sleeve(62)
                .tottalength(50)
                .build();

        CreateItemRequestDto createDto = CreateItemRequestDto.builder()
                .name(itemName)
                .categoryId(testCategory.getId().intValue())
                .price(price)
                .description(description)
                .directTrade(directTrade)
                .sizes(sizes)
                .imagePaths(Arrays.asList("http://path/to/image1.jpg", "http://path/to/image2.jpg"))
                .build();

        // when
        Item createdItem = itemUseCase.createItem(createDto, testMember.getMemberId());
        ItemDetailResponseDto foundItemDto = itemUseCase.findItemDetailById(createdItem.getId());

        // then
        assertThat(foundItemDto).isNotNull();
        assertThat(foundItemDto.getItemName()).isEqualTo(itemName);
        assertThat(foundItemDto.getItemPrice()).isEqualTo(price);
        assertThat(foundItemDto.getDescription()).isEqualTo(description);
        assertThat(foundItemDto.isDirectTrade()).isEqualTo(directTrade);
        assertThat(foundItemDto.getCategoryIds()).contains(testCategory.getId());

        assertThat(foundItemDto.getSizes()).isNotNull();
        assertThat(foundItemDto.getSizes().getChest()).isEqualTo(sizes.getChest());
        assertThat(foundItemDto.getSizes().getShoulder()).isEqualTo(sizes.getShoulder());
        assertThat(foundItemDto.getSizes().getSleeve()).isEqualTo(sizes.getSleeve());
        assertThat(foundItemDto.getSizes().getTotalLength()).isEqualTo(sizes.getTottalength());

        assertThat(foundItemDto.getImages()).hasSize(2);
        assertThat(foundItemDto.getImages().get(0).getImageUrl()).isEqualTo("http://path/to/image1.jpg");
    }
}
