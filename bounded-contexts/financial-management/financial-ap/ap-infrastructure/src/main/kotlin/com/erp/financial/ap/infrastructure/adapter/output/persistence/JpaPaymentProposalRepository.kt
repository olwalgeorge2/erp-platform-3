package com.erp.financial.ap.infrastructure.adapter.output.persistence

import com.erp.financial.ap.application.port.output.ListPaymentProposalsQuery
import com.erp.financial.ap.application.port.output.PaymentProposalRepository
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposal
import com.erp.financial.ap.infrastructure.persistence.entity.PaymentProposalEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaPaymentProposalRepository(
    private val entityManager: EntityManager,
) : PaymentProposalRepository {
    override fun save(proposal: PaymentProposal): PaymentProposal {
        val managed = entityManager.find(PaymentProposalEntity::class.java, proposal.id)
        val entity =
            if (managed == null) {
                PaymentProposalEntity.from(proposal).also(entityManager::persist)
            } else {
                managed.updateFrom(proposal)
                managed
            }
        entityManager.flush()
        return entity.toDomain()
    }

    override fun findById(
        tenantId: UUID,
        proposalId: UUID,
    ): PaymentProposal? =
        entityManager
            .find(PaymentProposalEntity::class.java, proposalId)
            ?.takeIf { it.tenantId == tenantId }
            ?.toDomain()

    override fun list(query: ListPaymentProposalsQuery): List<PaymentProposal> {
        val builder = StringBuilder("SELECT p FROM PaymentProposalEntity p WHERE p.tenantId = :tenantId")
        query.companyCodeId?.let { builder.append(" AND p.companyCodeId = :companyCodeId") }
        query.status?.let { builder.append(" AND p.status = :status") }
        builder.append(" ORDER BY p.proposalDate DESC")

        val jpaQuery =
            entityManager
                .createQuery(builder.toString(), PaymentProposalEntity::class.java)
                .setParameter("tenantId", query.tenantId)
        query.companyCodeId?.let { jpaQuery.setParameter("companyCodeId", it) }
        query.status?.let { jpaQuery.setParameter("status", it) }
        return jpaQuery.resultList.map(PaymentProposalEntity::toDomain)
    }
}
