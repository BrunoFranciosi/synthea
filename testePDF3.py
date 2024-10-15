import os
import json
from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas
from textwrap import wrap

# Função para garantir que não haja diagnósticos repetidos
def remove_duplicates(data_list):
    return list(set(data_list))

# Função para ler os dados de um arquivo JSON do Synthea e extrair altura, peso e pressão arterial
def read_patient_data(json_file):
    with open(json_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    patient = None
    hypotheses = []
    definitive_diagnoses = []
    treatments = []
    height = None
    weight = None
    bp = None  # Pressão arterial
    anamnesis = []

    # Extraindo dados do paciente, condições, tratamentos e medições
    for entry in data['entry']:
        resource = entry['resource']
        if resource['resourceType'] == 'Patient':
            patient = resource
        elif resource['resourceType'] == 'Condition':
            # Identificar o status da condição
            clinical_status = resource.get('clinicalStatus', {}).get('coding', [{}])[0].get('code', '')
            verification_status = resource.get('verificationStatus', {}).get('coding', [{}])[0].get('code', '')
            diagnosis_text = resource['code']['text']
            
            # Atualizando a lógica para capturar diagnósticos definitivos
            if clinical_status == 'provisional':  # Hipóteses Diagnósticas
                hypotheses.append(diagnosis_text)
            elif clinical_status in ['confirmed', 'active', 'resolved']:  # Diagnósticos Definitivos mais flexíveis
                definitive_diagnoses.append(diagnosis_text)
                
            anamnesis.append(diagnosis_text)  # Usar diagnósticos como anamnese (sintomas relatados)
        
        elif resource['resourceType'] == 'MedicationRequest':
            # Usando .get() para evitar o erro de chave ausente
            medication = resource.get('medicationCodeableConcept', {}).get('text', 'Tratamento desconhecido')
            treatments.append(medication)  # Tratamentos
        elif resource['resourceType'] == 'Observation':
            # Verificar se a observação é de altura, peso ou pressão arterial
            if 'valueQuantity' in resource:
                value = resource['valueQuantity']['value']
                unit = resource['valueQuantity']['unit']
                if 'height' in resource['code']['text'].lower():
                    height = f"{value} {unit}"
                elif 'weight' in resource['code']['text'].lower():
                    weight = f"{value} {unit}"
                elif 'blood pressure' in resource['code']['text'].lower():
                    bp = f"{value} {unit}"

    if patient:
        name = patient['name'][0]['given'][0] + ' ' + patient['name'][0]['family']
        gender = patient['gender']
        birth_date = patient['birthDate']
        age = 2024 - int(birth_date.split('-')[0])  # Calcular idade aproximada

        # Se não houver dados de altura, peso ou pressão arterial, definir "Não disponível"
        last_height = height if height else "Não disponível"
        last_weight = weight if weight else "Não disponível"
        last_bp = bp if bp else "Não disponível"
        anamnesis_text = ", ".join(anamnesis) if anamnesis else "Anamnese não disponível"

        # Retornar as informações relevantes
        return {
            'name': name,
            'age': age,
            'gender': gender,
            'anamnesis': anamnesis_text,
            'last_height': last_height,
            'last_weight': last_weight,
            'last_bp': last_bp,
            'hypotheses': hypotheses,
            'definitive_diagnoses': definitive_diagnoses,
            'treatments': treatments
        }

    return None


# Função para gerar um PDF a partir dos dados extraídos
def generate_pdf_from_data(patient_info):
    c = canvas.Canvas(f"{patient_info['name']}_Prontuario_Medico.pdf", pagesize=letter)
    width, height = letter
    margin = 40
    current_y = height - margin
    line_height = 15  # Altura da linha
    max_line_length = 90  # Máximo de caracteres por linha antes de quebrar

    # Função para controlar a posição do texto e quebrar páginas
    def draw_text(text, font_size=12, bold=False):
        nonlocal current_y
        if bold:
            c.setFont('Helvetica-Bold', font_size)
        else:
            c.setFont('Helvetica', font_size)

        lines = wrap(text, max_line_length)  # Quebra de linha automática

        for line in lines:
            if current_y < margin + line_height:  # Quebrar página se necessário
                c.showPage()
                current_y = height - margin
                c.setFont('Helvetica', font_size)
            c.drawString(margin, current_y, line)
            current_y -= line_height

    # Cabeçalho do PDF
    draw_text(f"Nome: {patient_info['name']}", bold=True)
    draw_text(f"Idade: {patient_info['age']}", bold=False)
    draw_text(f"Gênero: {patient_info['gender']}", bold=False)

    # Anamnese
    draw_text("Anamnese:", font_size=14, bold=True)
    draw_text(f"{patient_info['anamnesis']}", font_size=12, bold=False)

    # Exame Físico
    draw_text("Exame Físico:", font_size=14, bold=True)
    draw_text(f"Última altura registrada: {patient_info['last_height']}", font_size=12, bold=False)
    draw_text(f"Último peso registrado: {patient_info['last_weight']}", font_size=12, bold=False)
    draw_text(f"Última pressão arterial registrada: {patient_info['last_bp']}", font_size=12, bold=False)

    # Hipóteses Diagnósticas
    draw_text("Hipóteses Diagnósticas:", font_size=14, bold=True)
    if patient_info['hypotheses']:
        for diagnosis in remove_duplicates(patient_info['hypotheses']):
            draw_text(f"- {diagnosis}", font_size=12, bold=False)
    else:
        draw_text("Nenhuma hipótese diagnóstica registrada", font_size=12, bold=False)

    # Diagnósticos Definitivos
    draw_text("Diagnósticos Definitivos:", font_size=14, bold=True)
    if patient_info['definitive_diagnoses']:
        for diagnosis in remove_duplicates(patient_info['definitive_diagnoses']):
            draw_text(f"- {diagnosis}", font_size=12, bold=False)
    else:
        draw_text("Nenhum diagnóstico definitivo registrado", font_size=12, bold=False)

    # Tratamentos Efetuados
    draw_text("Tratamentos Efetuados:", font_size=14, bold=True)
    if patient_info['treatments']:
        for treatment in remove_duplicates(patient_info['treatments']):
            draw_text(f"- {treatment}", font_size=12, bold=False)
    else:
        draw_text("Nenhum tratamento registrado", font_size=12, bold=False)

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
