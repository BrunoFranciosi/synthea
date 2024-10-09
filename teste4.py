import json
import os
import random
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer



# Função para carregar dados do Synthea
def carregar_dados_synthea(diretorio_output):
    pacientes = []
    for filename in os.listdir(diretorio_output):
        if filename.endswith(".json"):
            with open(os.path.join(diretorio_output, filename)) as f:
                data = json.load(f)
                pacientes.append(data)  # Adiciona o paciente à lista
    return pacientes


# Função para gerar o PDF
def gerar_prontuario_pdf(dados_paciente, numero):
    pdf_file = f"prontuario_eletronico_{numero}.pdf"
    documento = SimpleDocTemplate(pdf_file, pagesize=letter)

    styles = getSampleStyleSheet()
    estilo_titulo = ParagraphStyle('Titulo', parent=styles['Heading1'], fontSize=16, spaceAfter=12)
    estilo_normal = styles['Normal']

    elementos = []

    # Título
    elementos.append(Paragraph("Prontuário Eletrônico", estilo_titulo))
    elementos.append(Spacer(1, 12))

    # Nome do paciente
    if 'patient' in dados_paciente and 'name' in dados_paciente['patient']:
        nome = dados_paciente['patient']['name'][0]['text']
        elementos.append(Paragraph(f"Nome: {nome}", estilo_normal))
    else:
        elementos.append(Paragraph("Nome: Não disponível", estilo_normal))

    # Data de nascimento
    if 'patient' in dados_paciente and 'birthDate' in dados_paciente['patient']:
        idade = dados_paciente['patient']['birthDate']
        elementos.append(Paragraph(f"Idade: {idade}", estilo_normal))
    else:
        elementos.append(Paragraph("Idade: Não disponível", estilo_normal))

    # Sexo
    if 'patient' in dados_paciente and 'gender' in dados_paciente['patient']:
        sexo = dados_paciente['patient']['gender']
        elementos.append(Paragraph(f"Sexo: {sexo}", estilo_normal))
    else:
        elementos.append(Paragraph("Sexo: Não disponível", estilo_normal))

    # Anamnese
    elementos.append(Paragraph("Anamnese:", estilo_normal))
    if 'observations' in dados_paciente:  # Ajuste a chave conforme necessário
        for obs in dados_paciente['observations']:  # Acesso às observações
            if 'code' in obs and 'text' in obs['code']:
                elementos.append(Paragraph(f"- {obs['code']['text']}", estilo_normal))
            else:
                elementos.append(Paragraph("- Informação não disponível", estilo_normal))
    else:
        elementos.append(Paragraph("- Não disponível", estilo_normal))

    # Exame Físico
    elementos.append(Paragraph("Exame Físico:", estilo_normal))
    if 'physicalExams' in dados_paciente:  # Ajuste a chave conforme necessário
        for exam in dados_paciente['physicalExams']:  # Acesso aos exames físicos
            if 'code' in exam and 'text' in exam['code']:
                elementos.append(Paragraph(f"- {exam['code']['text']}", estilo_normal))
            else:
                elementos.append(Paragraph("- Informação não disponível", estilo_normal))
    else:
        elementos.append(Paragraph("- Não disponível", estilo_normal))

    # Hipóteses Diagnósticas
    elementos.append(Paragraph("Hipóteses Diagnósticas:", estilo_normal))
    if 'diagnoses' in dados_paciente:  # Ajuste a chave conforme necessário
        for diag in dados_paciente['diagnoses']:  # Acesso aos diagnósticos
            if 'code' in diag and 'text' in diag['code']:
                elementos.append(Paragraph(f"- {diag['code']['text']}", estilo_normal))
            else:
                elementos.append(Paragraph("- Informação não disponível", estilo_normal))
    else:
        elementos.append(Paragraph("- Não disponível", estilo_normal))

    # Diagnósticos Definitivos
    elementos.append(Paragraph("Diagnósticos Definitivos:", estilo_normal))
    if 'finalDiagnoses' in dados_paciente:  # Ajuste a chave conforme necessário
        for final_diag in dados_paciente['finalDiagnoses']:  # Acesso aos diagnósticos definitivos
            if 'code' in final_diag and 'text' in final_diag['code']:
                elementos.append(Paragraph(f"- {final_diag['code']['text']}", estilo_normal))
            else:
                elementos.append(Paragraph("- Informação não disponível", estilo_normal))
    else:
        elementos.append(Paragraph("- Não disponível", estilo_normal))

    # Tratamentos Efetuados
    elementos.append(Paragraph("Tratamentos Efetuados:", estilo_normal))
    if 'treatments' in dados_paciente:  # Ajuste a chave conforme necessário
        for treatment in dados_paciente['treatments']:  # Acesso aos tratamentos
            if 'code' in treatment and 'text' in treatment['code']:
                elementos.append(Paragraph(f"- {treatment['code']['text']}", estilo_normal))
            else:
                elementos.append(Paragraph("- Informação não disponível", estilo_normal))
    else:
        elementos.append(Paragraph("- Não disponível", estilo_normal))

    # Constrói o PDF
    documento.build(elementos)

# Diretório onde os dados do Synthea foram gerados
diretorio_output = 'output/fhir'  # Mude para o caminho do seu diretório de saída
pacientes = carregar_dados_synthea(diretorio_output)

# Gerar múltiplos PDFs
num_pdfs = 5  # Número de PDFs a serem gerados
for i in range(num_pdfs):
    paciente_aleatorio = random.choice(pacientes)  # Selecionar um paciente aleatório
    gerar_prontuario_pdf(paciente_aleatorio, i + 1)  # Gerar o PDF com os dados do paciente

