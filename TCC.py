import json
import glob

# Caminho dos arquivos gerados pelo Synthea
files = glob.glob('output/fhir/*.json')

for file in files:
    with open(file, 'r') as f:
        data = json.load(f)
        
        # Pegar informações do paciente
        patient = None
        for entry in data['entry']:
            if entry['resource']['resourceType'] == 'Patient':
                patient = entry['resource']
                break
        
        if patient:
            # Extração segura do nome
            given_name = patient['name'][0].get('given', [''])[0]
            family_name = patient['name'][0].get('family', '')
            name = f"{given_name} {family_name}"
            
            gender = patient.get('gender', 'desconhecido')
            birth_date = patient.get('birthDate', 'desconhecida')

            # Calcular idade
            if birth_date != 'desconhecida':
                birth_year = int(birth_date.split('-')[0])
                age = 2024 - birth_year  # Use o ano atual para calcular a idade
            else:
                age = 'desconhecida'

            # Pegar diagnósticos (condições médicas)
            conditions = []
            for entry in data['entry']:
                if entry['resource']['resourceType'] == 'Condition':
                    condition = entry['resource']['code'].get('text', 'Desconhecida')
                    conditions.append(condition)

            print(f"Paciente: {name}, Gênero: {gender}, Idade: {age}")
            print("Diagnósticos:", conditions)
        else:
            print("Nenhum paciente encontrado nesse arquivo.")
