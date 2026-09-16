package dev.agner.chameidor.usecase.auth

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class AuthorizationServiceTest : StringSpec({

    val repository = mockk<IExternalSystemRepository>()
    val service = AuthorizationService(repository)

    val system = ExternalSystem(name = "valoab", active = true)
    val token = "valoab-dev-fixture-token"

    "authorize resolves an active system from its credential" {
        runTest {
            coEvery { repository.findByTokenHash(TokenHasher.sha256(token)) } returns system

            service.authorize(caller = null, target = "/tasks/one-time", credential = token) shouldBe
                AuthorizationResult.Authorized(system)
        }
    }

    "authorize accepts a caller claim matching the credential's system" {
        runTest {
            coEvery { repository.findByTokenHash(TokenHasher.sha256(token)) } returns system

            service.authorize(caller = "valoab", target = "/tasks/one-time", credential = token) shouldBe
                AuthorizationResult.Authorized(system)
        }
    }

    "authorize rejects a caller claim conflicting with the credential's system" {
        runTest {
            coEvery { repository.findByTokenHash(TokenHasher.sha256(token)) } returns system

            service.authorize(caller = "portfolio", target = "/tasks/one-time", credential = token) shouldBe
                AuthorizationResult.Unauthorized(AuthorizationResult.INVALID_CREDENTIAL)
        }
    }

    "authorize rejects missing or blank credentials without touching the registry" {
        runTest {
            val untouchedRegistry = mockk<IExternalSystemRepository>()
            val untouchingService = AuthorizationService(untouchedRegistry)

            untouchingService.authorize(caller = null, target = "/tasks/one-time", credential = null) shouldBe
                AuthorizationResult.Unauthorized(AuthorizationResult.MISSING_CREDENTIAL)
            untouchingService.authorize(caller = null, target = "/tasks/one-time", credential = "  ") shouldBe
                AuthorizationResult.Unauthorized(AuthorizationResult.MISSING_CREDENTIAL)

            coVerify(exactly = 0) { untouchedRegistry.findByTokenHash(any()) }
        }
    }

    "authorize rejects a credential that is not in the registry" {
        runTest {
            coEvery { repository.findByTokenHash(TokenHasher.sha256(token)) } returns null

            service.authorize(caller = null, target = "/tasks/one-time", credential = token) shouldBe
                AuthorizationResult.Unauthorized(AuthorizationResult.INVALID_CREDENTIAL)
        }
    }

    "authorize forbids an inactive system" {
        runTest {
            coEvery { repository.findByTokenHash(TokenHasher.sha256(token)) } returns system.copy(active = false)

            service.authorize(caller = null, target = "/tasks/one-time", credential = token) shouldBe
                AuthorizationResult.Forbidden(AuthorizationResult.INACTIVE_SYSTEM)
        }
    }
})
