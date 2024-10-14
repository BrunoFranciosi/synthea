import json
import os
import random
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer

# Função para carregar os pacientes gerados pelo Synthea a partir de arquivos JSON
def carregar_pacientes(caminho_pasta):
    pacientes = []
    for nome_arquivo in os.listdir(caminho_pasta):
        if nome_arquivo.endswith('.json'):  # Apenas arquivos JSON
            with open(os.path.join(caminho_pasta, nome_arquivo), 'r') as arquivo_json:
                dados_paciente = json.load(arquivo_json)
                pacientes.append(dados_paciente)
    return pacientes

# Função para gerar o PDF do prontuário
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
    if 'patient' in dados_paciente and 'name' in dados_paciente['patient'] and len(dados_paciente['patient']['name']) > 0:
        nome = dados_paciente['patient']['name'][0].get('text', 'Não disponível')
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
    if 'observations' in dados_paciente:
        for obs in dados_paciente['observations']:
            if 'code' in obs and 'text' in obs['code']:
                elementos.append(Paragraph(f"- {obs['code']['text']}", estilo_normal))
            else:
                elementos.append(Paragraph("- Informação não disponível", estilo_normal))
    else:
        elementos.append(Paragraph("- Não disponível", estilo_normal))

    # Salvando o PDF
    documento.build(elementos)

# Função para gerar e salvar o JSON dos dados do paciente
def salvar_dados_paciente_json(dados_paciente):
    with open('dados_paciente.json', 'w') as json_file:
        json.dump(dados_paciente, json_file, indent=4)
    print("Dados do paciente salvos no arquivo 'dados_paciente.json'")

# Carregar os pacientes da pasta gerada pelo Synthea
caminho_dados = 'output\metadata'  # Substitua pelo caminho correto
pacientes = carregar_pacientes(caminho_dados)

# Certifique-se de que a lista de pacientes não está vazia
if len(pacientes) == 0:
    print("Nenhum paciente encontrado. Verifique o caminho e os arquivos gerados.")
else:
    # Gerar PDFs para 5 pacientes aleatórios
    for i in range(5):
        paciente_aleatorio = random.choice(pacientes)
        gerar_prontuario_pdf(paciente_aleatorio, i + 1)
        salvar_dados_paciente_json(paciente_aleatorio)
