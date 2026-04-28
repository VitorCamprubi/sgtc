package com.vitorcamprubi.sgtc.repo;

import com.vitorcamprubi.sgtc.domain.Reuniao;
import com.vitorcamprubi.sgtc.domain.ReuniaoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
<<<<<<< HEAD
import java.util.Optional;
=======
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88

public interface ReuniaoRepository extends JpaRepository<Reuniao, Long> {
    List<Reuniao> findByGrupoIdOrderByDataHoraDesc(Long grupoId);
    List<Reuniao> findByGrupoIdAndStatusOrderByNumeroEncontroAscEncerradaEmAsc(Long grupoId, ReuniaoStatus status);
    void deleteByGrupoId(Long grupoId);
    long countByCriadoPorId(Long criadoPorId);
    boolean existsByGrupoIdAndStatusAndNumeroEncontro(Long grupoId, ReuniaoStatus status, Integer numeroEncontro);
<<<<<<< HEAD
    Optional<Reuniao> findByTokenConfirmacao(String tokenConfirmacao);
=======
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88

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
