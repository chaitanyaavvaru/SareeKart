package com.sareekart.dto.trousseau;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TrousseauDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Stage 3 DTO Test 1: CreateTrousseauBoardRequest valid and invalid bounds")
    void testCreateTrousseauBoardRequestValidation() {
        CreateTrousseauBoardRequest validReq = CreateTrousseauBoardRequest.builder()
                .title("Ananya & Siddharth Wedding")
                .weddingDate(LocalDate.now().plusMonths(6))
                .notes("Grand traditional wedding")
                .isPublicVoting(true)
                .build();
        Set<ConstraintViolation<CreateTrousseauBoardRequest>> violations = validator.validate(validReq);
        assertTrue(violations.isEmpty(), "Valid board request should produce no violations");

        CreateTrousseauBoardRequest blankTitleReq = CreateTrousseauBoardRequest.builder()
                .title("   ")
                .build();
        Set<ConstraintViolation<CreateTrousseauBoardRequest>> titleViolations = validator.validate(blankTitleReq);
        assertFalse(titleViolations.isEmpty(), "Blank title must fail validation");

        CreateTrousseauBoardRequest pastDateReq = CreateTrousseauBoardRequest.builder()
                .title("Past Wedding")
                .weddingDate(LocalDate.now().minusDays(5))
                .build();
        Set<ConstraintViolation<CreateTrousseauBoardRequest>> dateViolations = validator.validate(pastDateReq);
        assertFalse(dateViolations.isEmpty(), "Past wedding date must fail validation");
    }

    @Test
    @DisplayName("Stage 3 DTO Test 2: CreateCeremonyRequest ceremonyType taxonomy and budget constraints")
    void testCreateCeremonyRequestValidation() {
        CreateCeremonyRequest validCeremony = CreateCeremonyRequest.builder()
                .ceremonyType("MUHURTHAM")
                .title("Traditional Muhurtham")
                .colorTheme("Temple Gold & Crimson")
                .targetBudget(new BigDecimal("150000.00"))
                .displayOrder(1)
                .build();
        assertTrue(validator.validate(validCeremony).isEmpty());

        CreateCeremonyRequest invalidType = CreateCeremonyRequest.builder()
                .ceremonyType("RANDOM_PARTY")
                .title("Invalid Type")
                .build();
        Set<ConstraintViolation<CreateCeremonyRequest>> typeViolations = validator.validate(invalidType);
        assertFalse(typeViolations.isEmpty(), "Invalid ceremonyType must fail pattern validation");

        CreateCeremonyRequest negativeBudget = CreateCeremonyRequest.builder()
                .ceremonyType("HALDI")
                .title("Haldi")
                .targetBudget(new BigDecimal("-500.00"))
                .build();
        Set<ConstraintViolation<CreateCeremonyRequest>> budgetViolations = validator.validate(negativeBudget);
        assertFalse(budgetViolations.isEmpty(), "Negative budget must fail validation");
    }

    @Test
    @DisplayName("Stage 3 DTO Test 3: AddCeremonyItemRequest requires non-null productId")
    void testAddCeremonyItemRequestValidation() {
        AddCeremonyItemRequest validItem = AddCeremonyItemRequest.builder()
                .productId(101L)
                .notes("Must check zari authenticity")
                .isAiRecommended(true)
                .build();
        assertTrue(validator.validate(validItem).isEmpty());

        AddCeremonyItemRequest nullProduct = AddCeremonyItemRequest.builder()
                .productId(null)
                .build();
        Set<ConstraintViolation<AddCeremonyItemRequest>> violations = validator.validate(nullProduct);
        assertFalse(violations.isEmpty(), "Null productId must fail validation");
    }

    @Test
    @DisplayName("Stage 3 DTO Test 4: InviteCollaboratorRequest role and contact validation")
    void testInviteCollaboratorRequestValidation() {
        InviteCollaboratorRequest validCollab = InviteCollaboratorRequest.builder()
                .name("Aunt Sunita")
                .phone("+919876543210")
                .email("sunita@example.com")
                .role("CO_CURATOR")
                .build();
        assertTrue(validator.validate(validCollab).isEmpty());

        InviteCollaboratorRequest invalidRole = InviteCollaboratorRequest.builder()
                .name("Sister")
                .role("SUPER_ADMIN")
                .build();
        Set<ConstraintViolation<InviteCollaboratorRequest>> roleViolations = validator.validate(invalidRole);
        assertFalse(roleViolations.isEmpty(), "Unsupported role must fail pattern validation");

        InviteCollaboratorRequest invalidEmail = InviteCollaboratorRequest.builder()
                .name("Brother")
                .email("not-an-email")
                .role("VOTER")
                .build();
        Set<ConstraintViolation<InviteCollaboratorRequest>> emailViolations = validator.validate(invalidEmail);
        assertFalse(emailViolations.isEmpty(), "Malformed email must fail validation");
    }

    @Test
    @DisplayName("Stage 3 DTO Test 5: CastVoteRequest reaction taxonomy and required voterName")
    void testCastVoteRequestValidation() {
        for (String reaction : new String[]{"LOVE", "LIKE", "PASS", "NEEDS_REVIEW"}) {
            CastVoteRequest validVote = CastVoteRequest.builder()
                    .voterName("Radhika")
                    .reaction(reaction)
                    .note("Looks lovely")
                    .build();
            assertTrue(validator.validate(validVote).isEmpty(), "Reaction " + reaction + " should be valid");
        }

        CastVoteRequest invalidReaction = CastVoteRequest.builder()
                .voterName("Radhika")
                .reaction("DISLIKE")
                .build();
        Set<ConstraintViolation<CastVoteRequest>> reactionViolations = validator.validate(invalidReaction);
        assertFalse(reactionViolations.isEmpty(), "Invalid reaction must fail pattern validation");

        CastVoteRequest blankVoter = CastVoteRequest.builder()
                .voterName("")
                .reaction("LOVE")
                .build();
        Set<ConstraintViolation<CastVoteRequest>> voterViolations = validator.validate(blankVoter);
        assertFalse(voterViolations.isEmpty(), "Blank voter name must fail validation");
    }

    @Test
    @DisplayName("Stage 3 DTO Test 6: UpdateCeremonyItemRequest status taxonomy validation")
    void testUpdateCeremonyItemRequestValidation() {
        for (String status : new String[]{"SHORTLISTED", "SELECTED", "IN_CART", "PURCHASED", "ARCHIVED"}) {
            UpdateCeremonyItemRequest valid = UpdateCeremonyItemRequest.builder()
                    .status(status)
                    .notes("Updated notes")
                    .build();
            assertTrue(validator.validate(valid).isEmpty(), "Status " + status + " should be valid");
        }

        UpdateCeremonyItemRequest invalidStatus = UpdateCeremonyItemRequest.builder()
                .status("REJECTED_BY_BUYER")
                .build();
        Set<ConstraintViolation<UpdateCeremonyItemRequest>> violations = validator.validate(invalidStatus);
        assertFalse(violations.isEmpty(), "Unrecognized status must fail pattern validation");
    }
}
