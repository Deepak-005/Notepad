package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Offline Notepad", appName)
  }

  @Test
  fun `test CryptoUtils encryption and decryption`() {
    val plainText = "Hello, this is a secret backup payload!"
    val password = "MySecurePassword123"

    // 1. Encrypt the plaintext
    val encryptedText = com.example.util.CryptoUtils.encrypt(plainText, password)
    org.junit.Assert.assertNotEquals(plainText, encryptedText)
    org.junit.Assert.assertTrue(encryptedText.isNotEmpty())

    // 2. Decrypt with correct password
    val decryptedText = com.example.util.CryptoUtils.decrypt(encryptedText, password)
    assertEquals(plainText, decryptedText)

    // 3. Attempt decryption with wrong password should fail
    try {
      com.example.util.CryptoUtils.decrypt(encryptedText, "WrongPassword")
      org.junit.Assert.fail("Decryption with wrong password should throw an exception")
    } catch (e: Exception) {
      // Expected exception (AEADBadTagException or cipher failure)
    }
  }
}
