import os
import json
from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas

# Função para ler os dados de um arquivo JSON do Synthea
def read_patient_data(json_file):
    with open(json_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    patient = None
    conditions = []
    treatments = []

    # Extraindo dados do paciente, condições e tratamentos
    for entry in data['entry']:
        resource = entry['resource']
        if resource['resourceType'] == 'Patient':
            patient = resource
        elif resource['resourceType'] == 'Condition':
            conditions.append(resource['code']['text'])  # Diagnósticos/Hipóteses
        elif resource['resourceType'] == 'MedicationRequest':
            # Usando .get() para evitar o erro de chave ausente
            medication = resource.get('medicationCodeableConcept', {}).get('text', 'Tratamento desconhecido')
            treatments.append(medication)  # Tratamentos

    if patient:
        name = patient['name'][0]['given'][0] + ' ' + patient['name'][0]['family']
        gender = patient['gender']
        birth_date = patient['birthDate']
        age = 2024 - int(birth_date.split('-')[0])  # Calcular idade aproximada

        # Dados fictícios para anamnese e exame físico (adapte conforme os dados que você encontrar no JSON)
        anamnesis = "Paciente relata dor no peito e fadiga durante atividades físicas."
        physical_exam = "Pressão arterial: 140/90 mmHg, Frequência cardíaca: 80 bpm."

        # Retornar as informações relevantes
        return {
            'name': name,
            'age': age,
            'gender': gender,
            'anamnesis': anamnesis,
            'physical_exam': physical_exam,
            'hypotheses': conditions,
            'definitive_diagnoses': conditions,  # Você pode separar melhor as hipóteses e diagnósticos definitivos
            'treatments': treatments
        }

    return None

# Função para gerar um PDF a partir dos dados extraídos
def generate_pdf_from_data(patient_info):
    c = canvas.Canvas(f"{patient_info['name']}_Prontuario_Medico.pdf", pagesize=letter)
    width, height = letter
    margin = 40
    current_y = height - margin

    # Função para controlar a posição do texto e quebrar páginas
    def draw_text(text, font_size=12):
        nonlocal current_y
        c.setFont('Helvetica', font_size)
        # Quebrar página se necessário
        if current_y < margin:
            c.showPage()
            current_y = height - margin
        c.drawString(margin, current_y, text)
        current_y -= 20

    # Cabeçalho do PDF
    draw_text(f"Nome: {patient_info['name']}")
    draw_text(f"Idade: {patient_info['age']}")
    draw_text(f"Gênero: {patient_info['gender']}")

    # Anamnese
    draw_text("Anamnese:")
    draw_text(f"{patient_info['anamnesis']}")

    # Exame Físico
    draw_text("Exame Físico:")
    draw_text(f"{patient_info['physical_exam']}")

    # Hipóteses Diagnósticas
    draw_text("Hipóteses Diagnósticas:")
    for diagnosis in patient_info['hypotheses']:
        draw_text(f"- {diagnosis}")

    # Diagnósticos Definitivos
    draw_text("Diagnósticos Definitivos:")
    for diagnosis in patient_info['definitive_diagnoses']:
        draw_text(f"- {diagnosis}")

    # Tratamentos Efetuados
    draw_text("Tratamentos Efetuados:")
    for treatment in patient_info['treatments']:
        draw_text(f"- {treatment}")

    c.save()

# Caminho para os arquivos JSON gerados pelo Synthea
directory = 'output/fhir'  # Altere para o diretório correto

# Listar os arquivos JSON na pasta
for filename in os.listdir(directory):
    if filename.endswith(".json"):
        json_file = os.path.join(directory, filename)

        # Extrair dados do paciente
        patient_info = read_patient_data(json_file)

        if patient_info:
            # Gerar o PDF para o paciente
            generate_pdf_from_data(patient_info)
            print(f"PDF gerado para {patient_info['name']}")
