export interface GrupoResumoDTO {
  id: number;
  titulo: string;
  materia: 'TG' | 'PTG';
  status: 'EM_CURSO' | 'APROVADO' | 'REPROVADO';
  notaFinal: number | null;
  arquivadoEm: string | null;
  orientadorId: number;
  orientadorNome: string;
  coorientadorId: number | null;
  coorientadorNome: string | null;
  totalMembros: number;
}
