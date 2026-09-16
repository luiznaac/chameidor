package dev.agner.chameidor.usecase.auth

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TokenHasherTest : StringSpec({

    "sha256 produces the standard lowercase hex digest" {
        TokenHasher.sha256("abc") shouldBe
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    }
})
