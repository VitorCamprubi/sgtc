package com.vitorcamprubi.sgtc.repo;

import com.vitorcamprubi.sgtc.domain.Reuniao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReuniaoRepository extends JpaRepository<Reuniao, Long> {
    List<Reuniao> findByGrupoIdOrderByDataHoraDesc(Long grupoId);
}
