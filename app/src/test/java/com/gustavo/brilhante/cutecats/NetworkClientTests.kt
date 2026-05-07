package com.gustavo.brilhante.cutecats

import com.gustavo.brilhante.cutecats.data.ItemDto
import com.gustavo.brilhante.cutecats.data.ItemService
import com.gustavo.brilhante.cutecats.data.NetworkClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NetworkClientTest {

    private lateinit var itemService: ItemService
    private lateinit var networkClient: NetworkClient

    @Before
    fun setUp() {
        itemService = mockk()
        networkClient = NetworkClient(itemService)
    }

    @Test
    fun `getItems returns list from service`() = runTest {
        // Arrange
        val mockItems = mockItemsDto()

        coEvery { itemService.getItems() } returns mockItems

        // Act
        val result = networkClient.getItems()

        // Assert
        Assert.assertEquals(2, result.size)
        assertEquals("url1.com", result[0].url)
        assertEquals("url2.com", result[1].url)
    }

    private fun mockItemsDto(): List<ItemDto> {
        val mockItems = listOf(
            ItemDto(
                id = "1",
                url = "url1.com",
                width = 100L,
                height = 100L
            ),
            ItemDto(
                id = "2",
                url = "url2.com",
                width = 100L,
                height = 100L
            )
        )
        return mockItems
    }

    @Test(expected = RuntimeException::class)
    fun `getItems throws exception when service fails`() = runTest {
        // Arrange
        coEvery { itemService.getItems() } throws RuntimeException("API error")

        // Act
        networkClient.getItems() // Should throw
    }

    @Test
    fun `getItems returns empty list when service returns empty`() = runTest {
        // Arrange
        coEvery { itemService.getItems() } returns emptyList()

        // Act & Assert
        assertEquals(0, networkClient.getItems().size)
    }
}
