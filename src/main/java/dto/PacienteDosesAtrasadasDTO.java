package dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class PacienteDosesAtrasadasDTO {
    public InfoPacienteDTO paciente;
    public List<String> dosesAtrasadas;
    public InfoVacinaDTO vacina;
}

