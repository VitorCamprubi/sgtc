export interface GrupoResumoDTO {
  id: number;
  titulo: string;
  materia: 'TG' | 'PTG';
  orientadorId: number;
  orientadorNome: string;
  coorientadorId: number | null;
  coorientadorNome: string | null;
  totalMembros: number;
}
