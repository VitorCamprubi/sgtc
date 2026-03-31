package com.vitorcamprubi.sgtc.repo;

import com.vitorcamprubi.sgtc.domain.Reuniao;
import com.vitorcamprubi.sgtc.domain.ReuniaoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReuniaoRepository extends JpaRepository<Reuniao, Long> {
    List<Reuniao> findByGrupoIdOrderByDataHoraDesc(Long grupoId);
    void deleteByGrupoId(Long grupoId);
    long countByCriadoPorId(Long criadoPorId);

    @Query("""
            select r from Reuniao r
            where r.grupo.id = :grupoId
              and (r.status = :status or r.status is null)
              and r.dataHora <= :limite
            """)
    List<Reuniao> findAtrasadasDoGrupo(
            @Param("grupoId") Long grupoId,
            @Param("status") ReuniaoStatus status,
            @Param("limite") LocalDateTime limite);

    @Query("""
            select r from Reuniao r
            where (r.status = :status or r.status is null)
              and r.dataHora <= :limite
            """)
    List<Reuniao> findAtrasadas(
            @Param("status") ReuniaoStatus status,
            @Param("limite") LocalDateTime limite);
}
