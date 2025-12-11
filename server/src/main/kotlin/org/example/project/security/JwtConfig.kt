package org.example.project.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.example.project.JWT_AUDIENCE
import org.example.project.JWT_EXP_MINUTES
import org.example.project.JWT_ISSUER
import org.example.project.JWT_SECRET
import java.util.Date
import java.time.Instant
import java.time.temporal.ChronoUnit

object JwtConfig {
    private val algorithm: Algorithm = Algorithm.HMAC256(JWT_SECRET)

    fun verifier() = JWT
        .require(algorithm)
        .withAudience(JWT_AUDIENCE)
        .withIssuer(JWT_ISSUER)
        .build()

    fun generateToken(userId: Int, email: String, role: String): String {
        val now = Instant.now()
        val expiresAt = Date.from(now.plus(JWT_EXP_MINUTES, ChronoUnit.MINUTES))
        return JWT.create()
            .withIssuer(JWT_ISSUER)
            .withAudience(JWT_AUDIENCE)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("role", role.uppercase())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(expiresAt)
            .sign(algorithm)
    }
}
