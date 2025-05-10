package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
public class InternshipApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ItemService itemService;

	private Item item1;
	private Item item2;

	@BeforeEach
	public void setup() {
		item1 = new Item(1L, "Item1", "Desc1", "NEW", "item1@email.com");
		item2 = new Item(2L, "Item2", "Desc2", "NEW", "item2@email.com");
	}

	@Test
	public void testGetAllItems() throws Exception {
		when(itemService.findAll()).thenReturn(Arrays.asList(item1, item2));

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	public void testGetItemByIdFound() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item1));

		mockMvc.perform(get("/api/items/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Item1"));
	}

	@Test
	public void testGetItemByIdNotFound() throws Exception {
		when(itemService.findById(999L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/items/999"))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testCreateItemValid() throws Exception {
		when(itemService.save(any(Item.class))).thenReturn(item1);

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(item1)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Item1"));
	}

	@Test
	public void testCreateItemInvalidEmail() throws Exception {
		Item badItem = new Item(null, "Item3", "Desc", "NEW", "bademail");

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(badItem)))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void testUpdateItemFound() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item1));
		when(itemService.save(any(Item.class))).thenReturn(item1);

		mockMvc.perform(put("/api/items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(item1)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Item1"));
	}

	@Test
	public void testUpdateItemNotFound() throws Exception {
		when(itemService.findById(999L)).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/items/999")
						.contentType(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(item1)))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testDeleteItem() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item1));
		when(itemService.save(any(Item.class))).thenReturn(item1);

		mockMvc.perform(delete("/api/items/1"))
				.andExpect(status().isNoContent());
	}

	@Test
	public void testDeleteItemNotFound() throws Exception {
		mockMvc.perform(delete("/api/items/999"))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testProcessItems() throws Exception {
		when(itemService.processItemsAsync()).thenReturn(CompletableFuture.completedFuture(Arrays.asList(item1, item2)));

		mockMvc.perform(get("/api/items/process"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}
}
