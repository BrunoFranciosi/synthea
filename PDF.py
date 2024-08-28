from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas

def generate_pdf(patient_info):
    c = canvas.Canvas("Prontuario_Medico.pdf", pagesize=letter)
    width, height = letter

    # Identificação do Paciente
    c.drawString(100, height - 100, f"Nome: {patient_info['name']}")
    c.drawString(100, height - 120, f"Idade: {patient_info['age']}")
    c.drawString(100, height - 140, f"Gênero: {patient_info['gender']}")

    # Anamnese
    c.drawString(100, height - 180, "Anamnese:")
    c.drawString(120, height - 200, f"{patient_info['anamnesis']}")

    # Exame Físico
    c.drawString(100, height - 240, "Exame Físico:")
    c.drawString(120, height - 260, f"{patient_info['physical_exam']}")

    # Hipóteses Diagnósticas
    c.drawString(100, height - 300, "Hipóteses Diagnósticas:")
    for diagnosis in patient_info['hypotheses']:
        c.drawString(120, height - 320, f"{diagnosis}")
        height -= 20

    # Diagnósticos Definitivos
    c.drawString(100, height - 360, "Diagnósticos Definitivos:")
    for diagnosis in patient_info['definitive_diagnoses']:
        c.drawString(120, height - 380, f"{diagnosis}")
        height -= 20

    # Tratamentos Efetuados
    c.drawString(100, height - 420, "Tratamentos Efetuados:")
    for treatment in patient_info['treatments']:
        c.drawString(120, height - 440, f"{treatment}")
        height -= 20

    c.save()

# Exemplo de dados
patient_info = {
    "name": "Debbi Mueller",
    "age": 32,
    "gender": "female",
    "anamnesis": "Histórico de sintomas relatados...",
    "physical_exam": "Resultado do exame físico...",
    "hypotheses": ["Hipótese 1", "Hipótese 2"],
    "definitive_diagnoses": ["Diagnóstico 1", "Diagnóstico 2"],
    "treatments": ["Tratamento 1", "Tratamento 2"]
}

generate_pdf(patient_info)
