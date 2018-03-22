package com.rapidsos.emergencydatasdk.locations

import com.google.gson.Gson
import com.rapidsos.emergencydatasdk.ACCESS_TOKEN_INVALID_RESPONSE
import com.rapidsos.emergencydatasdk.BaseUnitTest
import com.rapidsos.emergencydatasdk.LOCATION_SUCCESS_RESPONSE
import com.rapidsos.emergencydatasdk.NOT_FOUND_RESPONSE
import com.rapidsos.emergencydatasdk.data.location.Location
import com.rapidsos.emergencydatasdk.data.network_response.SessionToken
import com.rapidsos.emergencydatasdk.internal.exceptions.EraSdkUnInitializedException
import com.rapidsos.emergencydatasdk.internal.exceptions.SessionTokenExpiredException
import com.rapidsos.emergencydatasdk.internal.helpers.dependency_injection.Injector
import com.rapidsos.emergencydatasdk.internal.helpers.network.NOT_FOUND_REQUEST_CODE
import com.rapidsos.emergencydatasdk.internal.helpers.network.OK_RESPONSE_CODE
import com.rapidsos.emergencydatasdk.internal.helpers.network.UNAUTHORIZED_RESPONSE_CODE
import com.rapidsos.emergencydatasdk.internal.preferences.EmgDataPreferences
import com.rapidsos.emergencydatasdk.rules.MockWebServerRule
import com.rapidsos.emergencydatasdk.rules.RxRule
import com.rapidsos.emergencydatasdk.sdk.SdkInitiatedValidator
import io.appflate.restmock.RESTMockServer
import io.appflate.restmock.utils.RequestMatchers
import io.reactivex.observers.TestObserver
import okhttp3.mockwebserver.MockResponse
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject

/**
 * @author John Hinton.
 */

class LocationRemoverTest : BaseUnitTest() {

    private val removeLocationsTokenMatcher = RequestMatchers
            .pathEndsWith("v1/rain/locations/${LocationRemoverTest.TEST_LOCATION_ID}")

    @Inject
    internal lateinit var preferences: EmgDataPreferences

    @Rule
    @JvmField
    var mockWebServerRule = MockWebServerRule()

    companion object {

        @ClassRule
        @JvmField
        var rxRule = RxRule()

        private const val ONE_DAY_MILLI_SECONDS = 86400000
        private const val ONE_HOUR_IN_SECONDS = 3600

        // The '1' at the end of this matcher can be the id of any request.
        // For testing purposes it will be 1
        private const val TEST_LOCATION_ID = 1
    }

    @Before
    fun setUpClass() {
        super.setUp()
        Injector.component = component
        component.inject(this)

        Mockito.`when`(preferences.getHost()).thenReturn(RESTMockServer.getUrl())
        Mockito.`when`(preferences.getClientId()).thenReturn("abc123")
        Mockito.`when`(preferences.getClientSecret()).thenReturn("123abc")
    }

    @Test
    fun testRemoveLocationsErrorSdkNotInit() {
        SdkInitiatedValidator.isSdkInit = false

        val mockResponse = MockResponse()
                .setBody(LOCATION_SUCCESS_RESPONSE)
                .setResponseCode(OK_RESPONSE_CODE)

        RESTMockServer.whenDELETE(removeLocationsTokenMatcher).thenReturn(mockResponse)

        val testObserver = TestObserver.create<Location>()
        val sessionToken = Mockito.mock(SessionToken::class.java)

        LocationRemover().remove(sessionToken, TEST_LOCATION_ID).subscribe(testObserver)

        with(testObserver) {
            awaitTerminalEvent()
            assertError(EraSdkUnInitializedException::class.java)
            assertErrorMessage("Error deleting " +
                    "location with id=$TEST_LOCATION_ID. Please make sure the SDK has" +
                    " been initialized.")
            assertNoValues()
        }
    }

    @Test
    fun testErrorRemovingLocationBecauseOfExpiredToken() {
        SdkInitiatedValidator.isSdkInit = true

        val mockResponse = MockResponse()
                .setBody(LOCATION_SUCCESS_RESPONSE)
                .setResponseCode(UNAUTHORIZED_RESPONSE_CODE)

        RESTMockServer.whenDELETE(removeLocationsTokenMatcher).thenReturn(mockResponse)

        val testObserver = TestObserver.create<Location>()

        val expiredSessionToken = SessionToken().apply {
            expiresIn = "$ONE_HOUR_IN_SECONDS"
            issuedAt = "${System.currentTimeMillis().minus(ONE_DAY_MILLI_SECONDS)}"
        }

        LocationRemover().remove(expiredSessionToken, TEST_LOCATION_ID).subscribe(testObserver)


        with(testObserver) {
            awaitTerminalEvent()
            assertError(SessionTokenExpiredException::class.java)
            assertErrorMessage("Cannot " +
                    "delete location with id=$TEST_LOCATION_ID. The session token provided is " +
                    "either invalid or expired.")
            assertNoValues()
        }
    }

    @Test
    fun testErrorRemovingLocationBecauseOfInvalidAccessToken() {
        SdkInitiatedValidator.isSdkInit = true

        val mockResponse = MockResponse()
                .setBody(ACCESS_TOKEN_INVALID_RESPONSE)
                .setResponseCode(UNAUTHORIZED_RESPONSE_CODE)

        RESTMockServer.whenDELETE(removeLocationsTokenMatcher).thenReturn(mockResponse)

        val testObserver = TestObserver.create<Location>()
        val invalidSessionToken = SessionToken().apply {
            expiresIn = "$ONE_HOUR_IN_SECONDS"
            issuedAt = "${System.currentTimeMillis().plus(ONE_DAY_MILLI_SECONDS)}"
        }

        LocationRemover().remove(invalidSessionToken, TEST_LOCATION_ID).subscribe(testObserver)

        with(testObserver) {
            awaitTerminalEvent()
            assertNoValues()
        }
    }

    @Test
    fun testRemovingLocationNotFound() {
        SdkInitiatedValidator.isSdkInit = true

        val mockResponse = MockResponse()
                .setBody(NOT_FOUND_RESPONSE)
                .setResponseCode(NOT_FOUND_REQUEST_CODE)

        RESTMockServer.whenDELETE(removeLocationsTokenMatcher).thenReturn(mockResponse)

        val testObserver = TestObserver.create<Location>()
        val sessionToken = SessionToken().apply {
            expiresIn = "$ONE_HOUR_IN_SECONDS"
            issuedAt = "${System.currentTimeMillis().plus(ONE_DAY_MILLI_SECONDS)}"
        }

        LocationRemover().remove(sessionToken, TEST_LOCATION_ID).subscribe(testObserver)

        with(testObserver) {
            awaitTerminalEvent()
            assertError { t: Throwable ->
                t.message?.contains(NOT_FOUND_RESPONSE) as Boolean
            }
        }
    }

    @Test
    fun testSuccessfulRemoveLocation() {
        SdkInitiatedValidator.isSdkInit = true

        val mockResponse = MockResponse()
                .setBody(LOCATION_SUCCESS_RESPONSE)
                .setResponseCode(OK_RESPONSE_CODE)

        RESTMockServer.whenDELETE(removeLocationsTokenMatcher).thenReturn(mockResponse)

        val testObserver = TestObserver.create<Location>()
        val sessionToken = SessionToken().apply {
            expiresIn = "$ONE_HOUR_IN_SECONDS"
            issuedAt = "${System.currentTimeMillis().plus(ONE_DAY_MILLI_SECONDS)}"
        }

        LocationRemover().remove(sessionToken, TEST_LOCATION_ID).subscribe(testObserver)

        val location: Location = Gson().fromJson(LOCATION_SUCCESS_RESPONSE, Location::class.java)

        with(testObserver) {
            awaitTerminalEvent()
            assertValue(location)
        }
    }

    @Test
    fun testRemovingLocationWithTimeout() {
        SdkInitiatedValidator.isSdkInit = true

        val mockResponse = MockResponse()
                .setBody(LOCATION_SUCCESS_RESPONSE)
                .setResponseCode(OK_RESPONSE_CODE)

        RESTMockServer.whenDELETE(removeLocationsTokenMatcher).thenReturn(mockResponse)

        val testObserver = TestObserver.create<Location>()
        val sessionToken = SessionToken().apply {
            expiresIn = "$ONE_HOUR_IN_SECONDS"
            issuedAt = "${System.currentTimeMillis().plus(ONE_DAY_MILLI_SECONDS)}"
        }

        LocationRemover().remove(sessionToken, TEST_LOCATION_ID).subscribe(testObserver)

        rxRule.testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)

        with(testObserver) {
            awaitTerminalEvent()
            assertError(TimeoutException::class.java)
            assertNoValues()
            assertNotComplete()
        }
    }
}