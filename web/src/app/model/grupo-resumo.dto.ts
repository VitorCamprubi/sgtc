export interface GrupoResumoDTO {
  id: number;
  titulo: string;
  orientadorNome: string;
  coorientadorNome: string | null;
  totalMembros: number;
}
