import { NavigatorScreenParams } from '@react-navigation/native';
import { ConsultancyService, JobVacancy } from '../types/mila';

export type ClientTabParamList = {
  ClientHome: undefined;
  ClientServices: undefined;
  ClientAbout: undefined;
  ClientEnquiry: { preselectedServiceType?: string; preselectedServiceId?: string } | undefined;
  ClientContact: undefined;
};

export type ClientStackParamList = {
  ClientTabs: NavigatorScreenParams<ClientTabParamList> | undefined;
  ServiceDetail: { service: ConsultancyService };
  ClientLogin: undefined;
  ClientRegister: undefined;
};

export type TalentTabParamList = {
  CareerHome: undefined;
  JobSearch: { initialCategory?: string } | undefined;
  Applications: undefined;
  CandidateProfile: undefined;
  AlertSettings: undefined;
};

export type TalentStackParamList = {
  TalentTabs: NavigatorScreenParams<TalentTabParamList> | undefined;
  JobDetail: { vacancy: JobVacancy };
  SavedJobs: undefined;
  CandidateLogin: undefined;
  CandidateRegister: undefined;
};
