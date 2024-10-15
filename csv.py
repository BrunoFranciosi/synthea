import pandas as pd
from fpdf import FPDF

# Carregar os dados gerados pelo Synthea
patients = pd.read_csv('synthea_sample_data_csv_apr2020/csv/patients.csv')
encounters = pd.read_csv('synthea_sample_data_csv_apr2020/csv/encounters.csv')
conditions = pd.read_csv('synthea_sample_data_csv_apr2020/csv/conditions.csv')
observations = pd.read_csv('synthea_sample_data_csv_apr2020/csv/observations.csv')
medications = pd.read_csv('synthea_sample_data_csv_apr2020/csv/medications.csv')

# Selecionar um paciente (exemplo: paciente com ID específico)
patient_id = '1d604da9-9a81-4ba9-80c2-de3375d59b40'  # Este é o ID de exemplo do seu CSV
patient_data = patients[patients['Id'] == patient_id]

# Função para gerar PDF do prontuário
def gerar_prontuario(patient_data):
    pdf = FPDF()
    pdf.add_page()

    # Identificação do paciente
    pdf.set_font("Arial", size=12)
    pdf.cell(200, 10, txt="Prontuário Eletrônico", ln=True, align='C')

    pdf.ln(10)  # linha em branco

    # Preencher com os dados do paciente
    pdf.cell(200, 10, txt=f"Nome: {patient_data['FIRST'].values[0]} {patient_data['LAST'].values[0]}", ln=True)
    pdf.cell(200, 10, txt=f"Sexo: {patient_data['GENDER'].values[0]}", ln=True)
    pdf.cell(200, 10, txt=f"Data de Nascimento: {patient_data['BIRTHDATE'].values[0]}", ln=True)
    pdf.cell(200, 10, txt=f"Raça: {patient_data['RACE'].values[0]}", ln=True)
    pdf.cell(200, 10, txt=f"Etnia: {patient_data['ETHNICITY'].values[0]}", ln=True)
    pdf.cell(200, 10, txt=f"Local de Nascimento: {patient_data['BIRTHPLACE'].values[0]}", ln=True)
    
    pdf.ln(10)

    # Adicionar seções do prontuário
    pdf.cell(200, 10, txt="Anamnese:", ln=True)
    # Aqui você pode preencher com as informações de 'encounters' e 'conditions' que sejam relevantes ao paciente
    encounter_data = encounters[encounters['PATIENT'] == patient_id]
    if not encounter_data.empty:
        encounter_info = f"Encontro em: {encounter_data['START'].values[0]} com código: {encounter_data['CODE'].values[0]}"
        pdf.cell(200, 10, txt=encounter_info, ln=True)
    else:
        pdf.cell(200, 10, txt="Nenhum encontro registrado.", ln=True)

    pdf.ln(10)
    pdf.cell(200, 10, txt="Exame Físico:", ln=True)
    # Preencher com observações, como pressão arterial, a partir de 'observations'
    observation_data = observations[observations['PATIENT'] == patient_id]
    if not observation_data.empty:
        # Verifica se a coluna 'UNIT' está presente
        if 'UNIT' in observation_data.columns:
            obs_info = f"Pressão arterial: {observation_data['VALUE'].values[0]} {observation_data['UNIT'].values[0]}"
        else:
            obs_info = f"Pressão arterial: {observation_data['VALUE'].values[0]}"
        pdf.cell(200, 10, txt=obs_info, ln=True)
    else:
        pdf.cell(200, 10, txt="Nenhuma observação registrada.", ln=True)

    pdf.ln(10)
    pdf.cell(200, 10, txt="Hipóteses Diagnósticas:", ln=True)
    # Extraído de 'conditions' ou 'encounters'
    condition_data = conditions[conditions['PATIENT'] == patient_id]
    if not condition_data.empty:
        condition_info = f"Condição: {condition_data['DESCRIPTION'].values[0]}"
        pdf.cell(200, 10, txt=condition_info, ln=True)
    else:
        pdf.cell(200, 10, txt="Nenhuma condição registrada.", ln=True)

    pdf.ln(10)
    pdf.cell(200, 10, txt="Diagnósticos Definitivos:", ln=True)
    # Lista final de diagnósticos (pode reutilizar a seção anterior ou adicionar algo novo)

    pdf.ln(10)
    pdf.cell(200, 10, txt="Tratamentos Efetuados:", ln=True)
    # Preencher com dados de 'medications' e 'procedures', se houver
    medication_data = medications[medications['PATIENT'] == patient_id]
    if not medication_data.empty:
        medication_info = f"Medicação: {medication_data['DESCRIPTION'].values[0]}"
        pdf.cell(200, 10, txt=medication_info, ln=True)
    else:
        pdf.cell(200, 10, txt="Nenhuma medicação registrada.", ln=True)

    # Salvar o PDF
    pdf.output("prontuario_eletronico.pdf")

# Gerar o prontuário para o paciente
gerar_prontuario(patient_data)
